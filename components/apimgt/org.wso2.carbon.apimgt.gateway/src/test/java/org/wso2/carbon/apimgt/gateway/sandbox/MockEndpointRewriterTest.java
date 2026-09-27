/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MockEndpointRewriterTest {

    private static final String NS = "http://ws.apache.org/ns/synapse";

    @Test
    public void rewritesGlobMatch() throws Exception {
        OMElement root = parse("<sequence xmlns=\"" + NS + "\">"
                + "<call blocking=\"true\"><endpoint><http method=\"post\" "
                + "uri-template=\"https://idp.example.com/token?x=1\"/></endpoint></call>"
                + "</sequence>");
        List<MockEndpointRewriter.Mock> mocks = List.of(
                new MockEndpointRewriter.Mock("m1", "https://idp.example.com/token*", "GLOB", null));
        List<String> warnings = new ArrayList<>();
        MockEndpointRewriter.rewrite(root, mocks, "http://127.0.0.1:8281/__apim_policy_sandbox/r1/mocks", warnings);

        String uri = findAttribute(root, "http", "uri-template");
        Assert.assertEquals("http://127.0.0.1:8281/__apim_policy_sandbox/r1/mocks/m1", uri);
        Assert.assertTrue(warnings.isEmpty());
    }

    @Test
    public void rewritesRegexMatch() throws Exception {
        OMElement root = parse("<sequence xmlns=\"" + NS + "\">"
                + "<call><endpoint><address uri=\"https://backend.example.com/v2/orders/1\"/></endpoint></call>"
                + "</sequence>");
        List<MockEndpointRewriter.Mock> mocks = List.of(
                new MockEndpointRewriter.Mock("m2", "https://backend\\.example\\.com/v2/orders/.*", "REGEX", null));
        List<String> warnings = new ArrayList<>();
        MockEndpointRewriter.rewrite(root, mocks, "http://127.0.0.1:8281/__apim_policy_sandbox/r1/mocks", warnings);

        String uri = findAttribute(root, "address", "uri");
        Assert.assertEquals("http://127.0.0.1:8281/__apim_policy_sandbox/r1/mocks/m2", uri);
    }

    @Test
    public void doesNotRewriteOnMethodMismatch() throws Exception {
        OMElement root = parse("<sequence xmlns=\"" + NS + "\">"
                + "<call><endpoint><http method=\"get\" uri-template=\"https://idp.example.com/token\"/></endpoint>"
                + "</call></sequence>");
        List<MockEndpointRewriter.Mock> mocks = List.of(
                new MockEndpointRewriter.Mock("m1", "https://idp.example.com/*", "GLOB", "POST"));
        List<String> warnings = new ArrayList<>();
        MockEndpointRewriter.rewrite(root, mocks, "http://127.0.0.1:8281/mocks", warnings);

        String uri = findAttribute(root, "http", "uri-template");
        Assert.assertEquals("https://idp.example.com/token", uri);
    }

    @Test
    public void rewritesWhenMockMethodIsBlank() throws Exception {
        OMElement root = parse("<sequence xmlns=\"" + NS + "\">"
                + "<call><endpoint><http method=\"post\" "
                + "uri-template=\"https://idp.example.com/token\"/></endpoint></call>"
                + "</sequence>");
        List<MockEndpointRewriter.Mock> mocks = List.of(
                new MockEndpointRewriter.Mock("m1", "https://idp.example.com/*", "GLOB", ""));
        List<String> warnings = new ArrayList<>();
        MockEndpointRewriter.rewrite(root, mocks, "http://127.0.0.1:8281/__apim_policy_sandbox/r1/mocks", warnings);

        String uri = findAttribute(root, "http", "uri-template");
        Assert.assertEquals("http://127.0.0.1:8281/__apim_policy_sandbox/r1/mocks/m1", uri);
        Assert.assertTrue(warnings.isEmpty());
    }

    @Test
    public void rewritesWhenMockMethodIsWhitespaceOnly() throws Exception {
        OMElement root = parse("<sequence xmlns=\"" + NS + "\">"
                + "<call><endpoint><http method=\"get\" "
                + "uri-template=\"https://idp.example.com/token\"/></endpoint></call>"
                + "</sequence>");
        List<MockEndpointRewriter.Mock> mocks = List.of(
                new MockEndpointRewriter.Mock("m1", "https://idp.example.com/*", "glob", "   "));
        List<String> warnings = new ArrayList<>();
        MockEndpointRewriter.rewrite(root, mocks, "http://127.0.0.1:8281/__apim_policy_sandbox/r1/mocks", warnings);

        String uri = findAttribute(root, "http", "uri-template");
        Assert.assertEquals("http://127.0.0.1:8281/__apim_policy_sandbox/r1/mocks/m1", uri);
        Assert.assertTrue(warnings.isEmpty());
    }

    @Test
    public void warnsOnRegistryKeyEndpoint() throws Exception {
        OMElement root = parse("<sequence xmlns=\"" + NS + "\">"
                + "<call><endpoint key=\"conf:/endpoints/backend\"/></call></sequence>");
        List<MockEndpointRewriter.Mock> mocks = List.of(
                new MockEndpointRewriter.Mock("m1", "*", "GLOB", null));
        List<String> warnings = new ArrayList<>();
        MockEndpointRewriter.rewrite(root, mocks, "http://127.0.0.1:8281/mocks", warnings);

        Assert.assertEquals(1, warnings.size());
        Assert.assertTrue(warnings.get(0).contains("conf:/endpoints/backend"));
    }

    @Test
    public void leavesNonMatchingUrlUnchanged() throws Exception {
        OMElement root = parse("<sequence xmlns=\"" + NS + "\">"
                + "<call><endpoint><http method=\"post\" uri-template=\"https://other.example.com/x\"/></endpoint>"
                + "</call></sequence>");
        List<MockEndpointRewriter.Mock> mocks = List.of(
                new MockEndpointRewriter.Mock("m1", "https://idp.example.com/*", "GLOB", null));
        List<String> warnings = new ArrayList<>();
        MockEndpointRewriter.rewrite(root, mocks, "http://127.0.0.1:8281/mocks", warnings);

        String uri = findAttribute(root, "http", "uri-template");
        Assert.assertEquals("https://other.example.com/x", uri);
        Assert.assertTrue(warnings.isEmpty());
    }

    private static OMElement parse(String xml) throws Exception {
        return AXIOMUtil.stringToOM(xml);
    }

    private static String findAttribute(OMElement root, String localName, String attrName) {
        Iterator<OMElement> children = root.getChildElements();
        while (children.hasNext()) {
            OMElement child = children.next();
            if (localName.equalsIgnoreCase(child.getLocalName())) {
                return child.getAttributeValue(new QName(attrName));
            }
            String nested = findAttribute(child, localName, attrName);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }
}

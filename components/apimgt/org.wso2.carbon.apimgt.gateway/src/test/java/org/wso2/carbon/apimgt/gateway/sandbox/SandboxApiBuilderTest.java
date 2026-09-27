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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SandboxApiBuilderTest {

    @Test
    public void mainApiHasExpectedShape() throws Exception {
        Map<String, String> extraProps = new LinkedHashMap<>();
        extraProps.put("api.ut.userName", "admin");
        String xml = SandboxApiBuilder.buildMainApiXml("run-1", "tok-123",
                "<log xmlns=\"http://ws.apache.org/ns/synapse\"/>", extraProps);
        OMElement api = AXIOMUtil.stringToOM(xml);

        Assert.assertEquals("__apim_policy_sandbox_run-1", api.getAttributeValue(new QName("name")));
        Assert.assertEquals("/__apim_policy_sandbox/run-1", api.getAttributeValue(new QName("context")));
        Assert.assertTrue(xml.contains("SandboxGuardMediator"));
        Assert.assertTrue(xml.contains("value=\"tok-123\""));
        Assert.assertTrue(xml.contains("api.ut.userName"));
        Assert.assertTrue(xml.contains("CaptureMediator"));
        Assert.assertTrue(xml.contains("phase\" value=\"in\""));
        Assert.assertTrue(xml.contains("phase\" value=\"out\""));
        Assert.assertTrue(xml.contains("phase\" value=\"fault\""));
        Assert.assertTrue(xml.contains("HTTP_SC\" value=\"403\""));
        Assert.assertTrue(xml.contains("HTTP_SC\" value=\"500\""));
        Assert.assertTrue(xml.contains("<respond/>"));
    }

    @Test
    public void mocksApiHasOneResourcePerMock() throws Exception {
        List<MockEndpointRewriter.Mock> mocks = List.of(
                new MockEndpointRewriter.Mock("m1", "*", "GLOB", null),
                new MockEndpointRewriter.Mock("m2", "*", "GLOB", null));
        Map<String, SandboxApiBuilder.MockConfig> configs = new LinkedHashMap<>();
        configs.put("m1", new SandboxApiBuilder.MockConfig(401, "application/json", "{\"error\":\"invalid\"}", 0));
        configs.put("m2", new SandboxApiBuilder.MockConfig(200, "application/json", "{}", 50));

        String xml = SandboxApiBuilder.buildMocksApiXml("run-1", mocks, configs);
        OMElement api = AXIOMUtil.stringToOM(xml);
        Assert.assertEquals("__apim_policy_sandbox_mocks_run-1", api.getAttributeValue(new QName("name")));
        Assert.assertEquals("/__apim_policy_sandbox/run-1/mocks", api.getAttributeValue(new QName("context")));

        Assert.assertTrue(xml.contains("url-mapping=\"/m1/*\""));
        Assert.assertTrue(xml.contains("url-mapping=\"/m2/*\""));
        Assert.assertTrue(xml.contains("mockId\" value=\"m1\""));
        Assert.assertTrue(xml.contains("status\" value=\"401\""));
        Assert.assertTrue(xml.contains("delayMs\" value=\"50\""));
        Assert.assertTrue(xml.contains("MockResponderMediator"));
    }

    @Test
    public void mocksApiSkipsMockWithoutConfig() throws Exception {
        List<MockEndpointRewriter.Mock> mocks = List.of(new MockEndpointRewriter.Mock("m1", "*", "GLOB", null));
        String xml = SandboxApiBuilder.buildMocksApiXml("run-1", mocks, new LinkedHashMap<>());
        Assert.assertFalse(xml.contains("url-mapping"));
    }
}

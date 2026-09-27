/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.util.AXIOMUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shares the editor's fixtures and expected node IDs, including both policy root shapes. */
public class SequenceInstrumenterTest {

    @Test
    public void matchesPublisherNodeIds() throws Exception {
        JsonObject allExpected = JsonParser.parseString(read("nodeIds.expected.json")).getAsJsonObject();
        for (Map.Entry<String, JsonElement> fixture : allExpected.entrySet()) {
            OMElement root = parseFixture(read(fixture.getKey()));
            new SequenceInstrumenter("test-run").instrument(root);
            Map<String, String> expected = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : fixture.getValue().getAsJsonObject().entrySet()) {
                expected.put(entry.getKey(), entry.getValue().getAsString());
            }
            Assert.assertEquals(fixture.getKey(), expected, markerTargets(root));
        }
    }

    @Test
    public void preservesOriginalMediatorCaseInTraceTag() throws Exception {
        // The UI diagram (and Editor/CONTRACT.md) render mediator names with their original casing
        // (e.g. "payloadFactory"), so trace[].tag must not be lowercased even though the
        // instrumenter still matches container mediators (filter/switch/...) case-insensitively.
        OMElement root = parseFixture(read("lean-auth-policy.mediators-only.xml"));
        new SequenceInstrumenter("test-run").instrument(root);
        Map<String, String> tags = markerTags(root);
        Assert.assertTrue("expected at least one payloadFactory tag, got: " + tags.values(),
                tags.values().contains("payloadFactory"));
        Assert.assertFalse("tag must not be lowercased", tags.values().contains("payloadfactory"));
    }

    @Test
    public void instrumentsAllDocumentedContainers() throws Exception {
        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "<clone><target><sequence><property name=\"a\"/></sequence></target>"
                + "<target><sequence><drop/></sequence></target></clone>"
                + "<iterate><target><sequence><respond/></sequence></target></iterate>"
                + "<foreach><target><sequence><log/></sequence></target></foreach>"
                + "<throttle><onAccept><property name=\"b\"/></onAccept>"
                + "<onReject><drop/></onReject></throttle>"
                + "<cache><onCacheHit><respond/></onCacheHit></cache>"
                + "<aggregate><onComplete><log/></onComplete></aggregate>"
                + "<validate><on-fail><drop/></on-fail></validate>"
                + "</sequence>";
        OMElement root = AXIOMUtil.stringToOM(xml);
        new SequenceInstrumenter("test-run").instrument(root);
        Map<String, String> targets = markerTargets(root);
        Assert.assertEquals("property", targets.get("0/target[0]/0"));
        Assert.assertEquals("drop", targets.get("0/target[1]/0"));
        Assert.assertEquals("respond", targets.get("1/target/0"));
        Assert.assertEquals("log", targets.get("2/target/0"));
        Assert.assertEquals("property", targets.get("3/onAccept/0"));
        Assert.assertEquals("drop", targets.get("3/onReject/0"));
        Assert.assertEquals("respond", targets.get("4/onCacheHit/0"));
        Assert.assertEquals("log", targets.get("5/onComplete/0"));
        Assert.assertEquals("drop", targets.get("6/on-fail/0"));
        Assert.assertEquals(16, targets.size());
    }

    private static OMElement parseFixture(String xml) throws Exception {
        String trimmed = xml.trim();
        if (!trimmed.startsWith("<sequence")) {
            trimmed = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\">"
                    + trimmed + "</sequence>";
        }
        return AXIOMUtil.stringToOM(trimmed);
    }

    private static Map<String, String> markerTargets(OMElement root) {
        Map<String, String> targets = new LinkedHashMap<>();
        collect(root, targets);
        return targets;
    }

    private static void collect(OMElement parent, Map<String, String> targets) {
        Iterator<OMElement> children = parent.getChildElements();
        while (children.hasNext()) {
            OMElement element = children.next();
            if ("class".equals(element.getLocalName())
                    && "org.wso2.carbon.apimgt.gateway.sandbox.TraceMarkerMediator".equals(
                    element.getAttributeValue(new QName("name")))) {
                String id = null;
                Iterator<OMElement> properties = element.getChildElements();
                while (properties.hasNext()) {
                    OMElement property = properties.next();
                    if ("nodeId".equals(property.getAttributeValue(new QName("name")))) {
                        id = property.getAttributeValue(new QName("value"));
                    }
                }
                OMNode sibling = element.getNextOMSibling();
                while (sibling != null && !(sibling instanceof OMElement)) {
                    sibling = sibling.getNextOMSibling();
                }
                Assert.assertNotNull("marker without a following mediator: " + id, sibling);
                Assert.assertNull("duplicate nodeId: " + id, targets.put(id, ((OMElement) sibling).getLocalName()));
            } else {
                collect(element, targets);
            }
        }
    }

    private static Map<String, String> markerTags(OMElement root) {
        Map<String, String> tags = new LinkedHashMap<>();
        collectTags(root, tags);
        return tags;
    }

    private static void collectTags(OMElement parent, Map<String, String> tags) {
        Iterator<OMElement> children = parent.getChildElements();
        while (children.hasNext()) {
            OMElement element = children.next();
            if ("class".equals(element.getLocalName())
                    && "org.wso2.carbon.apimgt.gateway.sandbox.TraceMarkerMediator".equals(
                    element.getAttributeValue(new QName("name")))) {
                String id = null;
                String tag = null;
                Iterator<OMElement> properties = element.getChildElements();
                while (properties.hasNext()) {
                    OMElement property = properties.next();
                    String propName = property.getAttributeValue(new QName("name"));
                    if ("nodeId".equals(propName)) {
                        id = property.getAttributeValue(new QName("value"));
                    } else if ("tag".equals(propName)) {
                        tag = property.getAttributeValue(new QName("value"));
                    }
                }
                tags.put(id, tag);
            } else {
                collectTags(element, tags);
            }
        }
    }

    private static String read(String name) throws IOException {
        try (InputStream input = SequenceInstrumenterTest.class.getResourceAsStream("/policy-sandbox/" + name)) {
            Assert.assertNotNull("fixture missing: " + name, input);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}

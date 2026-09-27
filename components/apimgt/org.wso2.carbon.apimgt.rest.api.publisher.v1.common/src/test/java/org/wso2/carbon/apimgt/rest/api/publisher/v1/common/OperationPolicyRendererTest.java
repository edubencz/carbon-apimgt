/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package org.wso2.carbon.apimgt.rest.api.publisher.v1.common;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OperationPolicyRendererTest {

    private final OperationPolicyRenderer renderer = new OperationPolicyRenderer();

    @Test
    public void rendersAndUnwrapsSequenceRoot() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("header_name", "X-Test");
        OperationPolicyRenderer.RenderOutput result = renderer.render(
                "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"original\">"
                        + "<header name=\"{{ header_name }}\" value=\"ok\"/>"
                        + "<respond/></sequence>", attributes);
        Assert.assertTrue(result.errors.toString(), result.errors.isEmpty());
        Assert.assertTrue(result.normalized);
        Assert.assertTrue(result.renderedSequence.contains("X-Test"));
        Assert.assertFalse(result.renderedSequence.contains("name=\"original\""));
        Assert.assertEquals(Collections.singletonList("header_name"), result.detectedVariables);
    }

    @Test
    public void acceptsMediatorFragmentWithoutNormalization() {
        OperationPolicyRenderer.RenderOutput result = renderer.render(
                "<property name=\"x\" value=\"1\"/><respond/>", Collections.emptyMap());
        Assert.assertTrue(result.errors.toString(), result.errors.isEmpty());
        Assert.assertFalse(result.normalized);
        Assert.assertTrue(result.renderedSequence.contains("respond"));
    }

    @Test
    public void reportsXmlErrorAtItsSourceLine() {
        OperationPolicyRenderer.RenderOutput result = renderer.render(
                "<property name=\"x\"/>\n<filter>\n</respond>", Collections.emptyMap());
        Assert.assertFalse(result.errors.isEmpty());
        Assert.assertTrue(result.errors.get(0).line >= 2);
    }

    @Test
    public void rejectsDoctypeAndExternalEntity() {
        OperationPolicyRenderer.RenderOutput result = renderer.render(
                "<!DOCTYPE root [<!ENTITY secret SYSTEM \"file:///etc/passwd\">]>"
                        + "<property name=\"x\" value=\"&secret;\"/>", Collections.emptyMap());
        Assert.assertFalse(result.errors.isEmpty());
    }
}

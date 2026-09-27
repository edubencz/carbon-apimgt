/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/** Verifies the sandbox contract's truncation limits: 64 KB per body, 4 KB per property value. */
public class CaptureMediatorTruncationTest {

    @Test
    public void shortValuesPassThroughUnchanged() {
        Assert.assertEquals("hello", CaptureMediator.truncate("hello", 64 * 1024));
    }

    @Test
    public void nullPassesThroughUnchanged() {
        Assert.assertNull(CaptureMediator.truncate(null, 64 * 1024));
    }

    @Test
    public void bodyLongerThan64KbIsTruncated() {
        String longBody = "x".repeat(70 * 1024);
        String result = CaptureMediator.truncate(longBody, 64 * 1024);
        Assert.assertTrue(result.endsWith("...(truncated)"));
        Assert.assertEquals(64 * 1024, result.length() - "...(truncated)".length());
    }

    @Test
    public void propertyValueLongerThan4KbIsTruncated() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("short", "ok");
        map.put("long", "y".repeat(5 * 1024));
        Map<String, String> truncated = CaptureMediator.truncateMap(map);

        Assert.assertEquals("ok", truncated.get("short"));
        Assert.assertTrue(truncated.get("long").endsWith("...(truncated)"));
        Assert.assertEquals(4 * 1024, truncated.get("long").length() - "...(truncated)".length());
    }
}

/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Exercises {@link PolicySandboxService}'s status-mapping rules against the sandbox contract. */
public class PolicySandboxServiceStatusMappingTest {

    @Test
    public void completedWhenCaptureReachedInPhase() {
        SandboxRunRegistry.RunRecord run = newRun();
        run.addTrace("0", "log");
        Map<String, Object> capture = new LinkedHashMap<>();
        capture.put("phase", "in");
        run.setCapture(capture);

        Assert.assertEquals("COMPLETED", PolicySandboxService.mapStatus(run));
    }

    @Test
    public void faultWhenCaptureReachedFaultPhase() {
        SandboxRunRegistry.RunRecord run = newRun();
        run.addTrace("10", "call");
        Map<String, Object> capture = new LinkedHashMap<>();
        capture.put("phase", "fault");
        run.setCapture(capture);
        run.setFault("101504", "Connection refused");

        Assert.assertEquals("FAULT", PolicySandboxService.mapStatus(run));
    }

    @Test
    public void respondedEarlyWhenPolicyOwnRespondRanBeforeCapture() {
        SandboxRunRegistry.RunRecord run = newRun();
        run.addTrace("0", "log");
        run.addTrace("3", "respond");
        // No capture: the policy's own <respond/> short-circuited mediation before our
        // own trailing CaptureMediator + <respond/> ever ran.

        Assert.assertEquals("RESPONDED", PolicySandboxService.mapStatus(run));
    }

    @Test
    public void droppedWhenLastNodeIsDrop() {
        SandboxRunRegistry.RunRecord run = newRun();
        run.addTrace("0", "log");
        run.addTrace("1", "drop");

        Assert.assertEquals("DROPPED", PolicySandboxService.mapStatus(run));
    }

    @Test
    public void filterNoiseHeadersDropsSandboxTokenAndLoopbackArtefacts() {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("X-APIM-Sandbox-Token", "secret-guard-token");
        headers.put("Host", "127.0.0.1:8281");
        headers.put("activityid", "abc-123");
        headers.put("Accept-Encoding", "gzip");
        headers.put("Connection", "keep-alive");
        headers.put("Transfer-Encoding", "chunked");
        headers.put("Date", "Fri, 26 Sep 2026 00:00:00 GMT");
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer xyz");

        Map<String, Object> filtered = PolicySandboxService.filterNoiseHeaders(headers);

        Assert.assertFalse(filtered.containsKey("X-APIM-Sandbox-Token"));
        Assert.assertFalse(filtered.containsKey("Host"));
        Assert.assertFalse(filtered.containsKey("activityid"));
        Assert.assertFalse(filtered.containsKey("Accept-Encoding"));
        Assert.assertFalse(filtered.containsKey("Connection"));
        Assert.assertFalse(filtered.containsKey("Transfer-Encoding"));
        Assert.assertFalse(filtered.containsKey("Date"));
        Assert.assertEquals("application/json", filtered.get("Content-Type"));
        Assert.assertEquals("Bearer xyz", filtered.get("Authorization"));
    }

    private static SandboxRunRegistry.RunRecord newRun() {
        return SandboxRunRegistry.create(UUID.randomUUID().toString(), 500);
    }
}

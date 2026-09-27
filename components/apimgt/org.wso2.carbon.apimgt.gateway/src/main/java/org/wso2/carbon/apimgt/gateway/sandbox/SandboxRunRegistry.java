/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory, process-local storage of everything captured for a single sandbox run, keyed by
 * runId. A run's record is created right before its temporary API is deployed and normally removed
 * in the orchestrator's {@code finally} block once the response has been collected; entries are
 * also subject to a TTL so a run record can never leak indefinitely even if cleanup is skipped
 * (e.g. the process is killed mid-run). (Formerly {@code SandboxRunStore}, Phase 1 spike.)
 */
public final class SandboxRunRegistry {

    /** How long an orphaned run record may live before {@link #sweepExpired()} reclaims it. */
    private static final long RUN_TTL_MS = 5 * 60 * 1000L;

    private static final Map<String, RunRecord> RUNS = new ConcurrentHashMap<>();

    private SandboxRunRegistry() {
    }

    public static RunRecord create(String runId, int maxLogLines) {
        RunRecord record = new RunRecord(runId, maxLogLines);
        RUNS.put(runId, record);
        return record;
    }

    public static RunRecord get(String runId) {
        return RUNS.get(runId);
    }

    public static void remove(String runId) {
        RUNS.remove(runId);
    }

    /** Removes run records older than {@link #RUN_TTL_MS}. Called by {@link SandboxJanitor}. */
    public static int sweepExpired() {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (Map.Entry<String, RunRecord> entry : RUNS.entrySet()) {
            if (now - entry.getValue().createdAt > RUN_TTL_MS) {
                RUNS.remove(entry.getKey());
                removed++;
            }
        }
        return removed;
    }

    /** Holds everything captured while a sandbox run is executing. */
    public static final class RunRecord {

        private final String runId;
        private final long createdAt = System.currentTimeMillis();
        private final int maxLogLines;
        private final AtomicInteger traceOrder = new AtomicInteger(0);
        private final List<Map<String, Object>> trace = new CopyOnWriteArrayList<>();
        private final List<Map<String, Object>> logs = new CopyOnWriteArrayList<>();
        /** FIFO of outbound calls not yet resolved by an AFTER marker / mock responder. */
        private final Deque<Map<String, Object>> pendingOutbound = new ArrayDeque<>();
        private final List<Map<String, Object>> outboundCalls = new CopyOnWriteArrayList<>();
        private final List<String> warnings = new CopyOnWriteArrayList<>();
        private volatile Map<String, Object> capture;
        private volatile String faultCode;
        private volatile String faultMessage;
        private volatile String lastNodeId;

        RunRecord(String runId, int maxLogLines) {
            this.runId = runId;
            this.maxLogLines = maxLogLines;
        }

        public String getRunId() {
            return runId;
        }

        public synchronized void addTrace(String nodeId, String tag) {
            lastNodeId = nodeId;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("order", traceOrder.getAndIncrement());
            entry.put("nodeId", nodeId);
            entry.put("tag", tag);
            entry.put("tMs", System.currentTimeMillis() - createdAt);
            trace.add(entry);
        }

        public synchronized void addLog(String nodeId, String level, String message) {
            if (logs.size() >= maxLogLines) {
                return;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("ts", System.currentTimeMillis());
            entry.put("level", level);
            entry.put("nodeId", nodeId);
            entry.put("message", message);
            logs.add(entry);
        }

        /** Enqueues a not-yet-resolved outbound call, recorded by the BEFORE half of a call/send. */
        public synchronized void beginOutboundCall(String nodeId, String method, String url) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("nodeId", nodeId);
            entry.put("method", method);
            entry.put("url", url);
            entry.put("mocked", false);
            entry.put("startedAt", System.currentTimeMillis());
            pendingOutbound.addLast(entry);
        }

        /** Resolves the oldest still-pending outbound call (FIFO), filling in the given fields. */
        public synchronized void resolveOldestOutboundCall(Integer status, String requestBody, Boolean mocked,
                                                            String mockId) {
            Map<String, Object> entry = pendingOutbound.pollFirst();
            if (entry == null) {
                return;
            }
            finishOutboundCall(entry, status, requestBody, mocked, mockId);
        }

        /** Resolves the outbound call matching {@code nodeId} (used by the blocking-call AFTER marker). */
        public synchronized void resolveOutboundCallByNodeId(String nodeId, Integer status) {
            Map<String, Object> match = null;
            for (Map<String, Object> entry : pendingOutbound) {
                if (nodeId.equals(entry.get("nodeId"))) {
                    match = entry;
                    break;
                }
            }
            if (match != null) {
                pendingOutbound.remove(match);
                finishOutboundCall(match, status, null, false, null);
            }
        }

        private void finishOutboundCall(Map<String, Object> entry, Integer status, String requestBody,
                                         Boolean mocked, String mockId) {
            long started = entry.get("startedAt") instanceof Long ? (Long) entry.get("startedAt")
                    : System.currentTimeMillis();
            entry.remove("startedAt");
            if (status != null) {
                entry.put("status", status);
            }
            if (requestBody != null) {
                entry.put("requestBody", requestBody);
            }
            if (mocked != null) {
                entry.put("mocked", mocked);
            }
            if (mockId != null) {
                entry.put("mockId", mockId);
            }
            entry.put("durationMs", System.currentTimeMillis() - started);
            outboundCalls.add(entry);
        }

        /** Drops any outbound calls that were begun but never resolved (e.g. dropped/timed out runs). */
        public synchronized void flushUnresolvedOutboundCalls() {
            Map<String, Object> entry;
            while ((entry = pendingOutbound.pollFirst()) != null) {
                finishOutboundCall(entry, null, null, null, null);
            }
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        public List<Map<String, Object>> getTrace() {
            return Collections.unmodifiableList(trace);
        }

        public List<Map<String, Object>> getLogs() {
            return Collections.unmodifiableList(logs);
        }

        public List<Map<String, Object>> getOutboundCalls() {
            return Collections.unmodifiableList(outboundCalls);
        }

        public void setCapture(Map<String, Object> capture) {
            this.capture = capture;
        }

        public Map<String, Object> getCapture() {
            return capture;
        }

        public String getLastNodeId() {
            return lastNodeId;
        }

        public void setFault(String code, String message) {
            this.faultCode = code;
            this.faultMessage = message;
        }

        public String getFaultCode() {
            return faultCode;
        }

        public String getFaultMessage() {
            return faultMessage;
        }
    }
}

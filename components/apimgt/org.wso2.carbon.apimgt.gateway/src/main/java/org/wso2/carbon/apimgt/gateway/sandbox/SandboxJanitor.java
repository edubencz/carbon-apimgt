/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Safety-net scheduled sweep that removes any {@code __apim_policy_sandbox_*} Synapse API still
 * deployed more than {@link #STALE_AFTER_MS} (5 minutes) after {@link PolicySandboxService}
 * recorded deploying it. {@link PolicySandboxService} always undeploys its own APIs in a
 * {@code finally} block; this only catches the case where that never ran (process/thread killed
 * mid-run, an uncaught error escaping the orchestrator, etc.) - it must never be relied on as the
 * primary cleanup path.
 */
public final class SandboxJanitor {

    private static final Log log = LogFactory.getLog(SandboxJanitor.class);
    private static final long STALE_AFTER_MS = 5 * 60 * 1000L;
    private static final long SWEEP_INTERVAL_S = 60L;

    private static final Map<String, Long> DEPLOYED_AT = new ConcurrentHashMap<>();
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static volatile ScheduledExecutorService executor;

    private SandboxJanitor() {
    }

    public static void trackDeployed(String apiName) {
        DEPLOYED_AT.put(apiName, System.currentTimeMillis());
    }

    public static void untrack(String apiName) {
        DEPLOYED_AT.remove(apiName);
    }

    /** Idempotent; safe to call from OSGi component activation every time the bundle starts. */
    public static synchronized void start() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "APIM-PolicySandbox-Janitor");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(SandboxJanitor::sweep, SWEEP_INTERVAL_S, SWEEP_INTERVAL_S, TimeUnit.SECONDS);
    }

    public static synchronized void stop() {
        if (!STARTED.compareAndSet(true, false)) {
            return;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    static void sweep() {
        try {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Long> entry : DEPLOYED_AT.entrySet()) {
                if (now - entry.getValue() > STALE_AFTER_MS) {
                    String apiName = entry.getKey();
                    log.warn("Sandbox janitor removing stale temporary API '" + apiName
                            + "' - it should have been undeployed by PolicySandboxService's own cleanup; "
                            + "this indicates that cleanup did not run for this run.");
                    try {
                        SynapseApiManager.undeploy(apiName);
                    } catch (Exception e) {
                        log.warn("Sandbox janitor failed to undeploy stale API '" + apiName + "'", e);
                    }
                    DEPLOYED_AT.remove(apiName);
                }
            }
            // Belt-and-braces: also sweep any __apim_policy_sandbox_* API present in the live
            // Synapse configuration that this janitor was never told about (e.g. after an
            // in-process restart lost DEPLOYED_AT) and is older than the runId's own embedded
            // creation is not derivable, so this pass is name-prefix only and TTL-blind; it exists
            // to bound worst-case leakage, not to be the primary cleanup signal.
            SandboxRunRegistry.sweepExpired();
        } catch (Exception e) {
            log.warn("Sandbox janitor sweep failed", e);
        }
    }
}

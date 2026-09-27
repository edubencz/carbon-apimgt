/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.wso2.carbon.apimgt.gateway.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.dto.OperationPolicySandboxConfig;
import org.wso2.carbon.utils.CarbonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Orchestrates one policy-sandbox execution end to end: renders the (already-rendered) sequence
 * into a temporary Synapse API, instruments it, deploys per-run mocks, invokes it over loopback,
 * collects trace/logs/final-message/outbound-calls, and undeploys everything - always, even on
 * error or timeout. See dev-scripts/sandbox-contract.md for the exact request/response shape this
 * builds towards, and the feature plan's "Fase 2" for the design this supersedes the Phase 1 spike
 * ({@code SpikeSandboxService}) with.
 */
public class PolicySandboxService {

    private static final Log log = LogFactory.getLog(PolicySandboxService.class);
    private static final long MAX_REQUEST_BODY_BYTES = 1024 * 1024L; // 1 MB, per the feature plan.

    private static volatile Semaphore semaphore;
    private static volatile int semaphorePermits = -1;
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "APIM-PolicySandbox-Run");
        thread.setDaemon(true);
        return thread;
    });

    /** Thrown to signal a request-level rejection the REST layer must map to a specific HTTP code. */
    public static final class RejectedException extends RuntimeException {
        public final int httpStatus;

        public RejectedException(int httpStatus, String message) {
            super(message);
            this.httpStatus = httpStatus;
        }
    }

    public Map<String, Object> execute(Map<String, Object> request) {

        OperationPolicySandboxConfig config = ServiceReferenceHolder.getInstance()
                .getAPIManagerConfiguration().getOperationPolicySandboxConfig();
        if (config == null || !config.isEnabled()) {
            throw new RejectedException(404, "Policy sandbox is disabled");
        }

        String renderedSequence = (String) request.get("renderedSequence");
        if (renderedSequence == null || renderedSequence.trim().isEmpty()) {
            throw new RejectedException(400, "renderedSequence is required");
        }
        if (renderedSequence.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_REQUEST_BODY_BYTES) {
            throw new RejectedException(413, "renderedSequence exceeds the maximum allowed size");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> sampleRequest = (Map<String, Object>) request.get("sampleRequest");
        String sampleBody = sampleRequest != null ? (String) sampleRequest.get("body") : null;
        if (sampleBody != null
                && sampleBody.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_REQUEST_BODY_BYTES) {
            throw new RejectedException(413, "sampleRequest.body exceeds the maximum allowed size");
        }

        ensureSemaphore(config.getMaxConcurrentRuns());
        if (!semaphore.tryAcquire()) {
            throw new RejectedException(429, "Too many concurrent policy sandbox runs");
        }

        String runId = UUID.randomUUID().toString();
        java.util.concurrent.Future<Map<String, Object>> future = EXECUTOR.submit(
                (Callable<Map<String, Object>>) () -> runOnce(runId, request, config));
        try {
            return future.get(config.getExecutionTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("Policy sandbox run " + runId + " exceeded execution_timeout ("
                    + config.getExecutionTimeout() + "ms); reporting TIMEOUT. Cleanup continues in the background.");
            Map<String, Object> timeoutResponse = new LinkedHashMap<>();
            timeoutResponse.put("status", "TIMEOUT");
            timeoutResponse.put("respondedEarly", false);
            timeoutResponse.put("durationMs", config.getExecutionTimeout());
            timeoutResponse.put("logs", java.util.Collections.emptyList());
            timeoutResponse.put("trace", java.util.Collections.emptyList());
            timeoutResponse.put("outboundCalls", java.util.Collections.emptyList());
            timeoutResponse.put("warnings", java.util.Collections.emptyList());
            timeoutResponse.put("errors", java.util.Collections.emptyList());
            return timeoutResponse;
        } catch (Exception e) {
            log.error("Policy sandbox run " + runId + " failed with an infrastructure error", e);
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("respondedEarly", false);
            errorResponse.put("logs", java.util.Collections.emptyList());
            errorResponse.put("trace", java.util.Collections.emptyList());
            errorResponse.put("outboundCalls", java.util.Collections.emptyList());
            errorResponse.put("warnings", java.util.Collections.emptyList());
            errorResponse.put("errors", java.util.List.of(String.valueOf(e.getMessage())));
            return errorResponse;
        }
        // NOTE: the semaphore permit is released by runOnce()'s own finally, on the worker thread -
        // not here - so a run that is still genuinely stuck past execution_timeout keeps counting
        // against max_concurrent_runs until it actually finishes, rather than silently letting more
        // in behind a caller that already gave up.
    }

    private static synchronized void ensureSemaphore(int permits) {
        if (semaphore == null || semaphorePermits != permits) {
            semaphore = new Semaphore(permits);
            semaphorePermits = permits;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> runOnce(String runId, Map<String, Object> request,
                                         OperationPolicySandboxConfig config) {

        long start = System.currentTimeMillis();
        String guardToken = UUID.randomUUID().toString() + UUID.randomUUID();
        SandboxRunRegistry.RunRecord run = SandboxRunRegistry.create(runId, config.getMaxLogLines());
        String mainApiName = SandboxApiBuilder.mainApiName(runId);
        String mocksApiName = SandboxApiBuilder.mocksApiName(runId);
        boolean mainDeployed = false;
        boolean mocksDeployed = false;
        List<String> errors = new ArrayList<>();
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            OMElement policyRoot;
            try {
                String renderedSequence = (String) request.get("renderedSequence");
                policyRoot = normalizeToSequenceRoot(renderedSequence);
            } catch (Exception e) {
                throw new RejectedException(400, "renderedSequence is not well-formed XML: " + e.getMessage());
            }

            new SequenceInstrumenter(runId).instrument(policyRoot);

            List<Map<String, Object>> mockDtos = (List<Map<String, Object>>) request.getOrDefault("mocks",
                    java.util.Collections.emptyList());
            List<MockEndpointRewriter.Mock> mocks = new ArrayList<>();
            Map<String, SandboxApiBuilder.MockConfig> mockConfigs = new LinkedHashMap<>();
            for (Map<String, Object> mockDto : mockDtos) {
                String id = (String) mockDto.get("id");
                mocks.add(new MockEndpointRewriter.Mock(id, (String) mockDto.get("urlPattern"),
                        (String) mockDto.get("matchType"), (String) mockDto.get("method")));
                Object statusObj = mockDto.get("status");
                int status = statusObj instanceof Number ? ((Number) statusObj).intValue() : 200;
                Object delayObj = mockDto.get("delayMs");
                long delayMs = delayObj instanceof Number ? ((Number) delayObj).longValue() : 0L;
                mockConfigs.put(id, new SandboxApiBuilder.MockConfig(status,
                        (String) mockDto.getOrDefault("contentType", "application/json"),
                        (String) mockDto.getOrDefault("body", "{}"), delayMs));
            }

            List<String> warnings = new ArrayList<>();
            int passthroughPort = getPassthroughHttpPort();
            String mocksBaseUrl = "http://127.0.0.1:" + passthroughPort + SandboxApiBuilder.mocksApiContext(runId);
            if (!mocks.isEmpty()) {
                MockEndpointRewriter.rewrite(policyRoot, mocks, mocksBaseUrl, warnings);
            }
            EndpointTimeoutInjector.inject(policyRoot, config.getExecutionTimeout());

            StringBuilder inSeq = new StringBuilder();
            java.util.Iterator<OMElement> children = policyRoot.getChildElements();
            while (children.hasNext()) {
                inSeq.append(children.next().toString());
            }

            @SuppressWarnings("unchecked")
            Map<String, String> extraProperties = (Map<String, String>) request.getOrDefault("extraProperties",
                    java.util.Collections.emptyMap());

            if (!mocks.isEmpty()) {
                String mocksApiXml = SandboxApiBuilder.buildMocksApiXml(runId, mocks, mockConfigs);
                SynapseApiManager.deploy(AXIOMUtil.stringToOM(mocksApiXml));
                mocksDeployed = true;
                SandboxJanitor.trackDeployed(mocksApiName);
            }

            String mainApiXml = SandboxApiBuilder.buildMainApiXml(runId, guardToken, inSeq.toString(),
                    extraProperties);
            SynapseApiManager.deploy(AXIOMUtil.stringToOM(mainApiXml));
            mainDeployed = true;
            SandboxJanitor.trackDeployed(mainApiName);

            @SuppressWarnings("unchecked")
            Map<String, Object> sampleRequest = (Map<String, Object>) request.getOrDefault("sampleRequest",
                    java.util.Collections.emptyMap());
            String method = (String) sampleRequest.getOrDefault("method", "POST");
            String path = (String) sampleRequest.getOrDefault("path", "/");
            String body = (String) sampleRequest.getOrDefault("body", "");
            @SuppressWarnings("unchecked")
            Map<String, String> headers = new LinkedHashMap<>((Map<String, String>) sampleRequest
                    .getOrDefault("headers", java.util.Collections.emptyMap()));
            headers.remove("content-length");
            headers.remove("host");
            headers.put("X-APIM-Sandbox-Token", guardToken);
            String contentType = (String) sampleRequest.get("contentType");
            if (contentType != null) {
                headers.putIfAbsent("Content-Type", contentType);
            }

            String url = "http://127.0.0.1:" + passthroughPort + SandboxApiBuilder.mainApiContext(runId) + path;
            LoopbackResponse loopback = send(url, method, body, headers, config.getExecutionTimeout());

            Map<String, Object> clientResponse = new LinkedHashMap<>();
            clientResponse.put("status", loopback.status);
            // filterNoiseHeaders: this is the literal HTTP response the loopback HttpClient
            // received from the passthrough transport's <respond/> - i.e. exactly what a real
            // caller would see - so it must be scrubbed the same way finalMessage/properties are.
            clientResponse.put("headers", filterNoiseHeaders(flattenHeaders(loopback.headers)));
            clientResponse.put("body", loopback.body);
            response.put("clientResponse", clientResponse);

            run.flushUnresolvedOutboundCalls();
            response.put("status", mapStatus(run));
            response.put("respondedEarly", "RESPONDED".equals(response.get("status")));
            response.put("finalMessage", buildFinalMessage(run));
            response.put("properties", buildProperties(run));
            response.put("logs", run.getLogs());
            response.put("trace", run.getTrace());
            response.put("outboundCalls", run.getOutboundCalls());
            if ("FAULT".equals(response.get("status"))) {
                Map<String, Object> fault = new LinkedHashMap<>();
                fault.put("code", run.getFaultCode());
                fault.put("message", run.getFaultMessage());
                fault.put("nodeId", run.getLastNodeId());
                response.put("fault", fault);
            }
            warnings.addAll(run.getWarnings());
            response.put("warnings", warnings);
            response.put("errors", errors);
            response.put("durationMs", System.currentTimeMillis() - start);
            return response;
        } catch (RejectedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Policy sandbox run " + runId + " failed", e);
            errors.add(String.valueOf(e.getMessage()));
            response.put("status", "ERROR");
            response.put("respondedEarly", false);
            response.put("durationMs", System.currentTimeMillis() - start);
            response.put("logs", run.getLogs());
            response.put("trace", run.getTrace());
            response.put("outboundCalls", run.getOutboundCalls());
            response.put("warnings", run.getWarnings());
            response.put("errors", errors);
            return response;
        } finally {
            if (mainDeployed) {
                SynapseApiManager.undeploy(mainApiName);
                SandboxJanitor.untrack(mainApiName);
            }
            if (mocksDeployed) {
                SynapseApiManager.undeploy(mocksApiName);
                SandboxJanitor.untrack(mocksApiName);
            }
            SandboxRunRegistry.remove(runId);
            semaphore.release();
        }
    }

    static String mapStatus(SandboxRunRegistry.RunRecord run) {
        Map<String, Object> capture = run.getCapture();
        if (capture != null && "fault".equals(capture.get("phase"))) {
            return "FAULT";
        }
        if (capture != null) {
            return "COMPLETED";
        }
        List<Map<String, Object>> trace = run.getTrace();
        if (!trace.isEmpty()) {
            Object lastTag = trace.get(trace.size() - 1).get("tag");
            if ("drop".equals(lastTag)) {
                return "DROPPED";
            }
            if ("respond".equals(lastTag)) {
                return "RESPONDED";
            }
        }
        return "RESPONDED";
    }

    static Map<String, Object> buildFinalMessage(SandboxRunRegistry.RunRecord run) {
        Map<String, Object> capture = run.getCapture();
        Map<String, Object> finalMessage = new LinkedHashMap<>();
        if (capture == null) {
            return finalMessage;
        }
        finalMessage.put("payload", capture.get("body"));
        finalMessage.put("contentType", capture.get("contentType"));
        finalMessage.put("httpStatus", capture.get("httpStatusCode"));
        finalMessage.put("headers", filterNoiseHeaders(capture.get("transportHeaders")));
        return finalMessage;
    }

    static Map<String, Object> buildProperties(SandboxRunRegistry.RunRecord run) {
        Map<String, Object> capture = run.getCapture();
        Map<String, Object> properties = new LinkedHashMap<>();
        if (capture == null) {
            properties.put("synapse", java.util.Collections.emptyMap());
            properties.put("axis2", java.util.Collections.emptyMap());
            properties.put("transport", java.util.Collections.emptyMap());
            return properties;
        }
        properties.put("synapse", capture.get("synapseProperties"));
        properties.put("axis2", capture.get("axis2Properties"));
        properties.put("transport", filterNoiseHeaders(capture.get("transportHeaders")));
        return properties;
    }

    /**
     * Headers stripped from every header map the response surfaces ({@code clientResponse.headers},
     * {@code finalMessage.headers}, {@code properties.transport}). Rule: drop anything that is an
     * artefact of the sandbox's own loopback plumbing - transport-level noise the passthrough
     * transport itself adds/rewrites ({@code Host}, {@code Connection}, {@code Accept-Encoding},
     * {@code Transfer-Encoding}, {@code Content-Length}, {@code Date}, {@code activityid}), plus the
     * guard credential the sandbox injects into the loopback request ({@code SANDBOX_TOKEN_HEADER}).
     * Keep everything else: headers the user's sample request genuinely carried (they would be
     * echoed by a real {@code <respond/>} too) and headers the policy itself set (e.g.
     * {@code Content-Type}, or {@code Authorization} set with transport scope) are real parts of the
     * response and must pass through unfiltered.
     */
    private static final java.util.Set<String> NOISE_HEADERS = java.util.Set.of(
            "host", "content-length", "connection", "accept-encoding", "user-agent",
            SandboxXmlUtil.SANDBOX_TOKEN_HEADER.toLowerCase(java.util.Locale.ROOT), "activityid", "date",
            "transfer-encoding");

    @SuppressWarnings("unchecked")
    static Map<String, Object> filterNoiseHeaders(Object headersObj) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (headersObj instanceof Map) {
            ((Map<String, Object>) headersObj).forEach((key, value) -> {
                if (!NOISE_HEADERS.contains(String.valueOf(key).toLowerCase(java.util.Locale.ROOT))) {
                    result.put(key, value);
                }
            });
        }
        return result;
    }

    private static Map<String, Object> flattenHeaders(Map<String, List<String>> headers) {
        Map<String, Object> result = new LinkedHashMap<>();
        headers.forEach((key, values) -> result.put(key, values.isEmpty() ? "" : values.get(0)));
        return result;
    }

    /**
     * Accepts either a normalized {@code <sequence>} root (unwrapped: name attribute discarded,
     * children promoted - the Control Plane's {@code OperationPolicyRenderer}/
     * {@code SequenceNormalizer} already do this) or a mediators-only fragment with no root at all,
     * and returns a fresh, synthetic {@code <sequence>} element wrapping the mediator children
     * either way, ready for {@link SequenceInstrumenter}.
     */
    static OMElement normalizeToSequenceRoot(String renderedSequence) throws Exception {
        String trimmed = renderedSequence.trim();
        OMElement parsed = AXIOMUtil.stringToOM(trimmed.startsWith("<sequence")
                ? trimmed
                : "<sequence xmlns=\"" + SandboxXmlUtil.SYNAPSE_NS + "\">" + trimmed + "</sequence>");
        return parsed;
    }

    private LoopbackResponse send(String url, String method, String body, Map<String, String> headers,
                                   int executionTimeoutMs) throws Exception {

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(executionTimeoutMs)
                .build();
        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
            HttpRequestBase httpRequest;
            String upperMethod = method == null ? "POST" : method.toUpperCase(java.util.Locale.ROOT);
            switch (upperMethod) {
                case "GET":
                    httpRequest = new org.apache.http.client.methods.HttpGet(url);
                    break;
                case "DELETE":
                    httpRequest = new org.apache.http.client.methods.HttpDelete(url);
                    break;
                case "PUT":
                    httpRequest = withEntity(new org.apache.http.client.methods.HttpPut(url), body);
                    break;
                case "PATCH":
                    httpRequest = withEntity(new org.apache.http.client.methods.HttpPatch(url), body);
                    break;
                case "HEAD":
                    httpRequest = new org.apache.http.client.methods.HttpHead(url);
                    break;
                case "OPTIONS":
                    httpRequest = new org.apache.http.client.methods.HttpOptions(url);
                    break;
                default:
                    httpRequest = withEntity(new org.apache.http.client.methods.HttpPost(url), body);
                    break;
            }
            headers.forEach(httpRequest::setHeader);
            try (CloseableHttpResponse httpResponse = client.execute(httpRequest)) {
                int status = httpResponse.getStatusLine().getStatusCode();
                String responseBody = httpResponse.getEntity() == null ? ""
                        : EntityUtils.toString(httpResponse.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
                Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
                for (Header header : httpResponse.getAllHeaders()) {
                    responseHeaders.computeIfAbsent(header.getName(), key -> new ArrayList<>())
                            .add(header.getValue());
                }
                return new LoopbackResponse(status, responseHeaders, responseBody);
            }
        }
    }

    private static HttpRequestBase withEntity(HttpEntityEnclosingRequestBase request, String body) {
        request.setEntity(new StringEntity(body == null ? "" : body, java.nio.charset.StandardCharsets.UTF_8));
        return request;
    }

    /**
     * See {@code SpikeSandboxService}'s (Phase 1) equivalent javadoc for why the "port" parameter
     * read off the live {@code TransportInDescription} at request time - not
     * {@code CarbonUtils.getTransportPort(...)}, and with no further offset arithmetic applied - is
     * the correct passthrough HTTP port to loopback against on this runtime.
     */
    private int getPassthroughHttpPort() {
        String override = System.getProperty("apim.sandbox.passthrough.port");
        if (override != null) {
            return Integer.parseInt(override);
        }
        try {
            org.apache.axis2.engine.AxisConfiguration axisConfig = ServiceReferenceHolder.getInstance()
                    .getConfigurationContextService().getServerConfigContext().getAxisConfiguration();
            org.apache.axis2.description.TransportInDescription transportIn =
                    axisConfig.getTransportIn(APIConstants.HTTP_PROTOCOL);
            return Integer.parseInt(transportIn.getParameter("port").getValue().toString().trim());
        } catch (Exception e) {
            log.warn("Could not resolve the passthrough HTTP port from AxisConfiguration, falling back to "
                    + "CarbonUtils.getTransportPort - set -Dapim.sandbox.passthrough.port to override", e);
            return CarbonUtils.getTransportPort(
                    ServiceReferenceHolder.getInstance().getConfigurationContextService(),
                    APIConstants.HTTP_PROTOCOL);
        }
    }

    private static final class LoopbackResponse {
        final int status;
        final Map<String, List<String>> headers;
        final String body;

        LoopbackResponse(int status, Map<String, List<String>> headers, String body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }
    }
}

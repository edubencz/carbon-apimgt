/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.wso2.carbon.apimgt.gateway.handlers.analytics.Constants;

import java.util.List;

/**
 * Builds the temporary Synapse API XML fragments the sandbox deploys per run:
 * <ul>
 *   <li>the "main" API, at {@code /__apim_policy_sandbox/<runId>}, whose inSequence is the guard +
 *   the instrumented policy under test + a final capture + respond;</li>
 *   <li>the per-run "mocks" API, at {@code /__apim_policy_sandbox/<runId>/mocks}, with one resource
 *   per configured mock (each a literal path segment, not a templated one, since all mock ids are
 *   known upfront).</li>
 * </ul>
 */
public final class SandboxApiBuilder {

    private SandboxApiBuilder() {
    }

    public static String mainApiName(String runId) {
        return "__apim_policy_sandbox_" + runId;
    }

    public static String mainApiContext(String runId) {
        return "/__apim_policy_sandbox/" + runId;
    }

    public static String mocksApiName(String runId) {
        return "__apim_policy_sandbox_mocks_" + runId;
    }

    public static String mocksApiContext(String runId) {
        return "/__apim_policy_sandbox/" + runId + "/mocks";
    }

    /**
     * @param instrumentedPolicyXml the already-instrumented (trace/outbound markers, mock-rewritten,
     *                              timeout-injected) policy sequence's children, serialized.
     */
    public static String buildMainApiXml(String runId, String guardToken, String instrumentedPolicyXml,
                                          java.util.Map<String, String> extraPropertiesXml) {

        StringBuilder extraProps = new StringBuilder();
        if (extraPropertiesXml != null) {
            extraPropertiesXml.forEach((name, value) -> extraProps.append("<property name=\"")
                    .append(SandboxXmlUtil.escapeXmlAttribute(name)).append("\" value=\"")
                    .append(SandboxXmlUtil.escapeXmlAttribute(value)).append("\" scope=\"default\"/>"));
        }

        return "<api xmlns=\"" + SandboxXmlUtil.SYNAPSE_NS + "\" name=\"" + mainApiName(runId) + "\" context=\""
                + mainApiContext(runId) + "\">"
                + "<resource methods=\"GET POST PUT DELETE PATCH HEAD OPTIONS\" url-mapping=\"/*\">"
                + "<inSequence>"
                // This synthetic, throwaway API is never registered with the control plane, so any
                // analytics/fault-event collection that tries to resolve it (AnalyticsMetricsHandler's
                // response-out-flow, which for a faulty response calls into
                // SubscriptionDataStoreImpl.getApiByContextAndVersion()) is guaranteed to miss the
                // local cache and fall through to a real, retried remote lookup - each retry sleeping
                // before the next attempt. That happens synchronously inside <respond/>'s own
                // Axis2Sender.sendBack() call, so the sandbox run hangs (well past execution_timeout,
                // since the retry/backoff schedule is its own, unrelated timeout) instead of returning
                // FAULT promptly. Skip it outright via the same, already-established
                // SKIP_METRICS_PUBLISHING escape hatch InMemoryAPIDeployer's health-check synthetic API
                // uses for the identical reason. Set first, before the guard, so it applies to every
                // exit path (forbidden/in/out/fault).
                + "<property name=\"" + Constants.SKIP_METRICS_PUBLISHING + "\" value=\"true\" type=\"BOOLEAN\"/>"
                + "<class name=\"org.wso2.carbon.apimgt.gateway.sandbox.SandboxGuardMediator\">"
                + "<property name=\"token\" value=\"" + guardToken + "\"/>"
                + "</class>"
                + "<filter source=\"$ctx:APIM_SANDBOX_AUTHORIZED\" regex=\"true\"><then>"
                // PTT-safe removal: a plain "remove" property mediator (as opposed to mutating the
                // live TRANSPORT_HEADERS map in place, e.g. via an iterator - see
                // SandboxGuardMediator's javadoc) keeps the guard credential out of everything
                // downstream: the instrumented policy under test, the outbound call to any mock, and
                // the eventual response.
                + "<property name=\"" + SandboxXmlUtil.SANDBOX_TOKEN_HEADER + "\" scope=\"transport\" "
                + "action=\"remove\"/>"
                + extraProps
                + instrumentedPolicyXml
                + "<class name=\"org.wso2.carbon.apimgt.gateway.sandbox.CaptureMediator\">"
                + "<property name=\"runId\" value=\"" + runId + "\"/>"
                + "<property name=\"phase\" value=\"in\"/></class>"
                + "<respond/>"
                + "</then><else>"
                + "<payloadFactory media-type=\"json\"><format>{\"error\":\"forbidden\"}</format>"
                + "<args/></payloadFactory>"
                + "<property name=\"HTTP_SC\" value=\"403\" scope=\"axis2\"/>"
                + "<respond/>"
                + "</else></filter>"
                + "</inSequence>"
                + "<outSequence>"
                + "<property name=\"" + SandboxXmlUtil.SANDBOX_TOKEN_HEADER + "\" scope=\"transport\" "
                + "action=\"remove\"/>"
                + "<class name=\"org.wso2.carbon.apimgt.gateway.sandbox.CaptureMediator\">"
                + "<property name=\"runId\" value=\"" + runId + "\"/>"
                + "<property name=\"phase\" value=\"out\"/></class>"
                + "<respond/>"
                + "</outSequence>"
                + "<faultSequence>"
                + "<property name=\"" + SandboxXmlUtil.SANDBOX_TOKEN_HEADER + "\" scope=\"transport\" "
                + "action=\"remove\"/>"
                + "<class name=\"org.wso2.carbon.apimgt.gateway.sandbox.CaptureMediator\">"
                + "<property name=\"runId\" value=\"" + runId + "\"/>"
                + "<property name=\"phase\" value=\"fault\"/></class>"
                + "<payloadFactory media-type=\"json\">"
                + "<format>{\"error\":true,\"code\":\"$1\",\"message\":\"$2\"}</format>"
                + "<args>"
                + "<arg evaluator=\"xml\" expression=\"$ctx:ERROR_CODE\"/>"
                + "<arg evaluator=\"xml\" expression=\"$ctx:ERROR_MESSAGE\"/>"
                + "</args>"
                + "</payloadFactory>"
                + "<property name=\"HTTP_SC\" value=\"500\" scope=\"axis2\"/>"
                + "<property name=\"NO_ENTITY_BODY\" scope=\"axis2\" action=\"remove\"/>"
                + "<respond/>"
                + "</faultSequence>"
                + "</resource>"
                + "</api>";
    }

    public static String buildMocksApiXml(String runId, List<MockEndpointRewriter.Mock> mocks,
                                           java.util.Map<String, MockConfig> mockConfigs) {
        StringBuilder resources = new StringBuilder();
        for (MockEndpointRewriter.Mock mock : mocks) {
            MockConfig config = mockConfigs.get(mock.id);
            if (config == null) {
                continue;
            }
            resources.append("<resource methods=\"GET POST PUT DELETE PATCH\" url-mapping=\"/")
                    .append(mock.id).append("/*\">")
                    .append("<inSequence>")
                    .append("<class name=\"org.wso2.carbon.apimgt.gateway.sandbox.MockResponderMediator\">")
                    .append("<property name=\"runId\" value=\"").append(runId).append("\"/>")
                    .append("<property name=\"mockId\" value=\"").append(mock.id).append("\"/>")
                    .append("<property name=\"status\" value=\"").append(config.status).append("\"/>")
                    .append("<property name=\"contentType\" value=\"")
                    .append(SandboxXmlUtil.escapeXmlAttribute(config.contentType)).append("\"/>")
                    .append("<property name=\"body\" value=\"")
                    .append(SandboxXmlUtil.escapeXmlAttribute(config.body)).append("\"/>")
                    .append("<property name=\"delayMs\" value=\"").append(config.delayMs).append("\"/>")
                    .append("</class>")
                    .append("<respond/>")
                    .append("</inSequence>")
                    .append("</resource>");
        }
        return "<api xmlns=\"" + SandboxXmlUtil.SYNAPSE_NS + "\" name=\"" + mocksApiName(runId) + "\" context=\""
                + mocksApiContext(runId) + "\">" + resources + "</api>";
    }

    /** Plain data holder for a mock's configured response, keyed by mock id. */
    public static final class MockConfig {
        public final int status;
        public final String contentType;
        public final String body;
        public final long delayMs;

        public MockConfig(int status, String contentType, String body, long delayMs) {
            this.status = status;
            this.contentType = contentType;
            this.body = body;
            this.delayMs = delayMs;
        }
    }
}

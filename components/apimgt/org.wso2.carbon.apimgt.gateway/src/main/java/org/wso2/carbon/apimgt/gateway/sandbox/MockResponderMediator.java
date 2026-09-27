/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Installed as the inSequence of a sandbox run's per-run mock API resource
 * ({@code /__apim_policy_sandbox/{runId}/mocks/m{i}}). Records the inbound request it received
 * (resolving the outbound call this mock stood in for, in {@link SandboxRunRegistry}) and then
 * answers with the configured status/headers/body/contentType, after an optional delay.
 *
 * <pre>{@code
 * <class name="org.wso2.carbon.apimgt.gateway.sandbox.MockResponderMediator">
 *     <property name="runId" value="..."/>
 *     <property name="mockId" value="m1"/>
 *     <property name="status" value="401"/>
 *     <property name="contentType" value="application/json"/>
 *     <property name="body" value="{&quot;error&quot;:&quot;invalid&quot;}"/>
 *     <property name="delayMs" value="0"/>
 * </class>
 * }</pre>
 */
public class MockResponderMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(MockResponderMediator.class);
    private static final int MAX_BODY_CHARS = 64 * 1024;

    private String runId;
    private String mockId;
    private int status = 200;
    private String contentType = "application/json";
    private String body = "{}";
    private long delayMs = 0;

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public void setMockId(String mockId) {
        this.mockId = mockId;
    }

    public void setStatus(String status) {
        this.status = Integer.parseInt(status.trim());
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setDelayMs(String delayMs) {
        this.delayMs = Long.parseLong(delayMs.trim());
    }

    @Override
    public boolean mediate(org.apache.synapse.MessageContext synCtx) {

        MessageContext axis2MC = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        SandboxRunRegistry.RunRecord run = SandboxRunRegistry.get(runId);
        String requestBody = readRequestBody(axis2MC);
        if (run != null) {
            run.resolveOldestOutboundCall(status, requestBody, true, mockId);
        }
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            String jsonBody = body;
            if (!isJsonContentType(contentType)) {
                // JsonUtil stores the raw string as the message's JSON payload regardless of its
                // actual media type; the ContentType/transport header set below is what actually
                // controls what the caller sees, so a non-JSON mock body still round-trips intact.
                jsonBody = body == null ? "" : body;
            }
            JsonUtil.getNewJsonPayload(axis2MC, jsonBody == null ? "{}" : jsonBody, true, true);
        } catch (Exception e) {
            log.warn("Mock responder (mock " + mockId + ") failed to set the response body", e);
        }
        axis2MC.setProperty(org.wso2.carbon.apimgt.gateway.APIMgtGatewayConstants.HTTP_SC, String.valueOf(status));
        // Both MESSAGE_TYPE and CONTENT_TYPE need to be set together (see AWSLambdaMediator's
        // proven pattern for this) - the passthrough transport's response formatter resolves the
        // actual outgoing Content-Type header from these axis2 properties at send time, not merely
        // from a TRANSPORT_HEADERS map entry, so setting only the latter left the receiving side
        // (the blocking <call>'s caller) unable to recognize the mocked response as JSON.
        axis2MC.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE, contentType);
        axis2MC.setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, contentType);
        applyContentTypeHeader(axis2MC);
        return true;
    }

    private static boolean isJsonContentType(String contentType) {
        return contentType != null && contentType.toLowerCase(java.util.Locale.ROOT).contains("json");
    }

    @SuppressWarnings("unchecked")
    private void applyContentTypeHeader(MessageContext axis2MC) {
        Object existing = axis2MC.getProperty(MessageContext.TRANSPORT_HEADERS);
        Map<String, Object> headers = existing instanceof Map
                ? new LinkedHashMap<>((Map<String, Object>) existing) : new LinkedHashMap<>();
        headers.put("Content-Type", contentType);
        axis2MC.setProperty(MessageContext.TRANSPORT_HEADERS, headers);
    }

    private String readRequestBody(MessageContext axis2MC) {
        try {
            if (JsonUtil.hasAJsonPayload(axis2MC)) {
                String value = JsonUtil.jsonPayloadToString(axis2MC);
                return truncate(value);
            }
            org.apache.synapse.MessageContext synCtx = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            org.apache.axiom.soap.SOAPEnvelope env = axis2MC.getEnvelope();
            if (env != null && env.getBody() != null && env.getBody().getFirstElement() != null) {
                env.getBody().getFirstElement().serialize(baos);
            }
            return truncate(baos.toString());
        } catch (Exception e) {
            log.debug("Could not read the inbound request body for mock " + mockId, e);
            return null;
        }
    }

    private static String truncate(String value) {
        if (value != null && value.length() > MAX_BODY_CHARS) {
            return value.substring(0, MAX_BODY_CHARS) + "...(truncated)";
        }
        return value;
    }

    @Override
    public String getType() {
        return "APIMPolicySandboxMockResponder";
    }
}

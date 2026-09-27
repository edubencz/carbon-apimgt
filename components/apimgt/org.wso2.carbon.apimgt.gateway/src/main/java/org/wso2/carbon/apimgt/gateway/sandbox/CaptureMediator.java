/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
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
 * Class mediator inserted right before the final &lt;respond/&gt; of the in/out/fault sequences of
 * the temporary sandbox API. Captures the final message state (JSON payload if present, else the
 * raw XML/text body), HTTP_SC, transport headers, and a curated set of Synapse/axis2 properties,
 * all truncated per the sandbox contract, and stores it into {@link SandboxRunRegistry} for the
 * current run so {@link PolicySandboxService} can build the REST response's {@code finalMessage}/
 * {@code properties} fields. On the fault sequence, also records {@code ERROR_CODE}/
 * {@code ERROR_MESSAGE} as the run's fault.
 *
 * <pre>{@code
 * <class name="org.wso2.carbon.apimgt.gateway.sandbox.CaptureMediator">
 *     <property name="runId" value="..."/>
 *     <property name="phase" value="in|out|fault"/>
 * </class>
 * }</pre>
 */
public class CaptureMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(CaptureMediator.class);
    private static final int MAX_BODY_CHARS = 64 * 1024;
    private static final int MAX_PROPERTY_CHARS = 4 * 1024;

    private String runId;
    private String phase;

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    @Override
    public boolean mediate(org.apache.synapse.MessageContext synCtx) {

        SandboxRunRegistry.RunRecord run = SandboxRunRegistry.get(runId);
        if (run == null) {
            return true;
        }
        try {
            Map<String, Object> capture = new LinkedHashMap<>();
            capture.put("phase", phase);

            MessageContext axis2MC = ((Axis2MessageContext) synCtx).getAxis2MessageContext();

            String body;
            boolean isJson = JsonUtil.hasAJsonPayload(axis2MC);
            if (isJson) {
                body = JsonUtil.jsonPayloadToString(axis2MC);
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    org.apache.axiom.soap.SOAPEnvelope env = synCtx.getEnvelope();
                    if (env != null && env.getBody() != null && env.getBody().getFirstElement() != null) {
                        env.getBody().getFirstElement().serialize(baos);
                    }
                } catch (Exception e) {
                    log.debug("Could not serialize non-JSON payload for sandbox capture", e);
                }
                body = baos.toString();
            }
            capture.put("body", truncate(body, MAX_BODY_CHARS));
            capture.put("isJson", isJson);
            capture.put("contentType", axis2MC.getProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE));
            capture.put("httpStatusCode", axis2MC.getProperty(
                    org.wso2.carbon.apimgt.gateway.APIMgtGatewayConstants.HTTP_SC));

            capture.put("transportHeaders", truncateMap(transportHeaders(axis2MC)));
            capture.put("axis2Properties", truncateMap(axis2Properties(axis2MC)));
            capture.put("synapseProperties", truncateMap(synapseProperties(synCtx)));

            run.setCapture(capture);

            if ("fault".equals(phase)) {
                Object code = synCtx.getProperty("ERROR_CODE");
                Object message = synCtx.getProperty("ERROR_MESSAGE");
                run.setFault(code == null ? "" : String.valueOf(code),
                        message == null ? "" : String.valueOf(message));
            }
        } catch (Exception e) {
            log.warn("Sandbox capture failed for run " + runId, e);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> transportHeaders(MessageContext axis2MC) {
        Object headers = axis2MC.getProperty(MessageContext.TRANSPORT_HEADERS);
        Map<String, String> result = new LinkedHashMap<>();
        if (headers instanceof Map) {
            ((Map<Object, Object>) headers).forEach((key, value) -> {
                // SandboxApiBuilder already removes this header (via a "remove" property mediator,
                // scope="transport") right after the guard filter and at the top of the out/fault
                // sequences, but this capture feeds both properties.transport and
                // finalMessage.headers, so it is excluded here too, defensively, in case it is ever
                // present (e.g. a future flow that captures before that removal runs).
                if (SandboxXmlUtil.SANDBOX_TOKEN_HEADER.equalsIgnoreCase(String.valueOf(key))) {
                    return;
                }
                result.put(String.valueOf(key), value == null ? "" : String.valueOf(value));
            });
        }
        return result;
    }

    /** A curated, known-safe-to-serialize subset of axis2 message context properties. */
    private Map<String, String> axis2Properties(MessageContext axis2MC) {
        Map<String, String> result = new LinkedHashMap<>();
        String[] keys = {"HTTP_SC", "HTTP_METHOD", "ContentType", "messageType", "REST_URL_POSTFIX",
                "NO_ENTITY_BODY"};
        for (String key : keys) {
            Object value = axis2MC.getProperty(key);
            if (value != null) {
                result.put(key, String.valueOf(value));
            }
        }
        return result;
    }

    private Map<String, String> synapseProperties(org.apache.synapse.MessageContext synCtx) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Object keyObj : synCtx.getPropertyKeySet()) {
            String key = String.valueOf(keyObj);
            Object value = synCtx.getProperty(key);
            if (value == null || !isSimple(value)) {
                continue;
            }
            result.put(key, String.valueOf(value));
        }
        return result;
    }

    private static boolean isSimple(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    static Map<String, String> truncateMap(Map<String, String> map) {
        Map<String, String> result = new LinkedHashMap<>();
        map.forEach((key, value) -> result.put(key, truncate(value, MAX_PROPERTY_CHARS)));
        return result;
    }

    static String truncate(String value, int maxChars) {
        if (value != null && value.length() > maxChars) {
            return value.substring(0, maxChars) + "...(truncated)";
        }
        return value;
    }

    @Override
    public String getType() {
        return "APIMPolicySandboxCapture";
    }
}

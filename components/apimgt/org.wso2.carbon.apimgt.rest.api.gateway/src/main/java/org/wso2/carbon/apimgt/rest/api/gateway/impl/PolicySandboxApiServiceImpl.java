/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.wso2.carbon.apimgt.rest.api.gateway.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.gateway.sandbox.PolicySandboxService;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.rest.api.common.RestApiCommonUtil;
import org.wso2.carbon.apimgt.rest.api.gateway.PolicySandboxApiService;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.ErrorDTO;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxExecuteRequestDTO;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxExecuteResponseDTO;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxMockDTO;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxSampleRequestDTO;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Production REST layer for the policy-editor-sandbox feature's gateway endpoint. Maps the
 * generated {@code PolicySandboxExecuteRequestDTO}/{@code PolicySandboxExecuteResponseDTO} to/from
 * the plain {@code Map}-based shape {@link PolicySandboxService} works with, and enforces the extra
 * admin-only gate this endpoint needs beyond the baseline Basic-Auth-valid-user check that
 * {@code BasicAuthenticationInterceptor} otherwise applies uniformly to every gateway v2 REST
 * resource (see that interceptor's {@code validateAdminPermission}, which is a no-op unless the
 * runtime's {@code deployment.toml} explicitly lists a URI under
 * {@code apim.rest_api.admin_permission_restricted_api} - true for every other resource in this
 * webapp too, not just this one). Because this endpoint executes arbitrary Synapse mediation with
 * real outbound network calls, it checks {@code /permission/admin/manage/apim_admin} directly here
 * instead of relying on that opt-in, deployment-configured mechanism.
 */
public class PolicySandboxApiServiceImpl implements PolicySandboxApiService {

    private static final Log log = LogFactory.getLog(PolicySandboxApiServiceImpl.class);
    private final PolicySandboxService service = new PolicySandboxService();

    @Override
    public Response executePolicySandbox(PolicySandboxExecuteRequestDTO body, MessageContext messageContext)
            throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            if (!APIUtil.hasPermission(username, APIConstants.Permissions.APIM_ADMIN)) {
                return error(Response.Status.FORBIDDEN, "Admin permission is required to use the policy sandbox");
            }
        } catch (APIManagementException e) {
            log.error("Could not verify admin permission for user " + username, e);
            return error(Response.Status.FORBIDDEN, "Admin permission is required to use the policy sandbox");
        }

        try {
            Map<String, Object> request = toRequestMap(body);
            Map<String, Object> result = service.execute(request);
            return Response.ok(toResponseDto(result)).build();
        } catch (PolicySandboxService.RejectedException e) {
            return error(Response.Status.fromStatusCode(e.httpStatus), e.getMessage());
        }
    }

    private Map<String, Object> toRequestMap(PolicySandboxExecuteRequestDTO body) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("renderedSequence", body.getRenderedSequence());
        request.put("flow", body.getFlow() == null ? "request" : body.getFlow().value());
        request.put("extraProperties", body.getExtraProperties());
        request.put("captureSnapshots", body.isCaptureSnapshots());

        PolicySandboxSampleRequestDTO sampleRequest = body.getSampleRequest();
        if (sampleRequest != null) {
            Map<String, Object> sampleRequestMap = new LinkedHashMap<>();
            sampleRequestMap.put("method", sampleRequest.getMethod());
            sampleRequestMap.put("path", sampleRequest.getPath());
            sampleRequestMap.put("headers", sampleRequest.getHeaders());
            sampleRequestMap.put("body", sampleRequest.getBody());
            sampleRequestMap.put("contentType", sampleRequest.getContentType());
            request.put("sampleRequest", sampleRequestMap);
        }

        List<Map<String, Object>> mocks = new ArrayList<>();
        if (body.getMocks() != null) {
            for (PolicySandboxMockDTO mock : body.getMocks()) {
                Map<String, Object> mockMap = new LinkedHashMap<>();
                mockMap.put("id", mock.getId());
                mockMap.put("urlPattern", mock.getUrlPattern());
                mockMap.put("matchType", mock.getMatchType() == null ? null : mock.getMatchType().value());
                mockMap.put("method", mock.getMethod());
                mockMap.put("status", mock.getStatus());
                mockMap.put("headers", mock.getHeaders());
                mockMap.put("body", mock.getBody());
                mockMap.put("contentType", mock.getContentType());
                mockMap.put("delayMs", mock.getDelayMs());
                mocks.add(mockMap);
            }
        }
        request.put("mocks", mocks);
        return request;
    }

    @SuppressWarnings("unchecked")
    private PolicySandboxExecuteResponseDTO toResponseDto(Map<String, Object> result) {
        PolicySandboxExecuteResponseDTO dto = new PolicySandboxExecuteResponseDTO();
        dto.setStatus(PolicySandboxExecuteResponseDTO.StatusEnum.fromValue((String) result.get("status")));
        dto.setRespondedEarly((Boolean) result.getOrDefault("respondedEarly", false));
        Object duration = result.get("durationMs");
        if (duration instanceof Number) {
            dto.setDurationMs(((Number) duration).longValue());
        }

        Map<String, Object> clientResponse = (Map<String, Object>) result.get("clientResponse");
        if (clientResponse != null) {
            org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxClientResponseDTO clientResponseDto =
                    new org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxClientResponseDTO();
            Object status = clientResponse.get("status");
            if (status instanceof Number) {
                clientResponseDto.setStatus(((Number) status).intValue());
            }
            clientResponseDto.setHeaders(stringifyMap((Map<String, Object>) clientResponse.get("headers")));
            clientResponseDto.setBody((String) clientResponse.get("body"));
            dto.setClientResponse(clientResponseDto);
        }

        Map<String, Object> finalMessage = (Map<String, Object>) result.get("finalMessage");
        if (finalMessage != null && !finalMessage.isEmpty()) {
            org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxFinalMessageDTO finalMessageDto =
                    new org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxFinalMessageDTO();
            finalMessageDto.setPayload((String) finalMessage.get("payload"));
            finalMessageDto.setContentType((String) finalMessage.get("contentType"));
            Object httpStatus = finalMessage.get("httpStatus");
            if (httpStatus != null) {
                try {
                    finalMessageDto.setHttpStatus(Integer.valueOf(String.valueOf(httpStatus)));
                } catch (NumberFormatException ignored) {
                    // leave unset
                }
            }
            finalMessageDto.setHeaders(stringifyMap((Map<String, Object>) finalMessage.get("headers")));
            dto.setFinalMessage(finalMessageDto);
        }

        Map<String, Object> properties = (Map<String, Object>) result.get("properties");
        if (properties != null) {
            org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxPropertiesDTO propertiesDto =
                    new org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxPropertiesDTO();
            propertiesDto.setSynapse(stringifyMap((Map<String, Object>) properties.get("synapse")));
            propertiesDto.setAxis2(stringifyMap((Map<String, Object>) properties.get("axis2")));
            propertiesDto.setTransport(stringifyMap((Map<String, Object>) properties.get("transport")));
            dto.setProperties(propertiesDto);
        }

        List<Map<String, Object>> logs = (List<Map<String, Object>>) result.getOrDefault("logs",
                java.util.Collections.emptyList());
        List<org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxLogEntryDTO> logDtos = new ArrayList<>();
        for (Map<String, Object> logEntry : logs) {
            org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxLogEntryDTO logDto =
                    new org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxLogEntryDTO();
            Object ts = logEntry.get("ts");
            if (ts instanceof Number) {
                logDto.setTs(((Number) ts).longValue());
            }
            logDto.setLevel((String) logEntry.get("level"));
            logDto.setNodeId((String) logEntry.get("nodeId"));
            logDto.setMessage((String) logEntry.get("message"));
            logDtos.add(logDto);
        }
        dto.setLogs(logDtos);

        List<Map<String, Object>> trace = (List<Map<String, Object>>) result.getOrDefault("trace",
                java.util.Collections.emptyList());
        List<org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxTraceEntryDTO> traceDtos = new ArrayList<>();
        for (Map<String, Object> traceEntry : trace) {
            org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxTraceEntryDTO traceDto =
                    new org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxTraceEntryDTO();
            Object order = traceEntry.get("order");
            if (order instanceof Number) {
                traceDto.setOrder(((Number) order).intValue());
            }
            traceDto.setNodeId((String) traceEntry.get("nodeId"));
            traceDto.setTag((String) traceEntry.get("tag"));
            Object tMs = traceEntry.get("tMs");
            if (tMs instanceof Number) {
                traceDto.setTMs(((Number) tMs).longValue());
            }
            traceDtos.add(traceDto);
        }
        dto.setTrace(traceDtos);

        List<Map<String, Object>> outboundCalls = (List<Map<String, Object>>) result.getOrDefault("outboundCalls",
                java.util.Collections.emptyList());
        List<org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxOutboundCallDTO> outboundCallDtos =
                new ArrayList<>();
        for (Map<String, Object> call : outboundCalls) {
            org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxOutboundCallDTO callDto =
                    new org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxOutboundCallDTO();
            callDto.setNodeId((String) call.get("nodeId"));
            callDto.setMethod((String) call.get("method"));
            callDto.setUrl((String) call.get("url"));
            callDto.setMocked((Boolean) call.getOrDefault("mocked", false));
            callDto.setMockId((String) call.get("mockId"));
            Object status = call.get("status");
            if (status instanceof Number) {
                callDto.setStatus(((Number) status).intValue());
            }
            callDto.setRequestBody((String) call.get("requestBody"));
            Object durationMs = call.get("durationMs");
            if (durationMs instanceof Number) {
                callDto.setDurationMs(((Number) durationMs).longValue());
            }
            outboundCallDtos.add(callDto);
        }
        dto.setOutboundCalls(outboundCallDtos);

        Map<String, Object> fault = (Map<String, Object>) result.get("fault");
        if (fault != null) {
            org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxFaultDTO faultDto =
                    new org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxFaultDTO();
            faultDto.setCode((String) fault.get("code"));
            faultDto.setMessage((String) fault.get("message"));
            faultDto.setNodeId((String) fault.get("nodeId"));
            dto.setFault(faultDto);
        }

        dto.setWarnings(new ArrayList<>((List<String>) result.getOrDefault("warnings",
                java.util.Collections.emptyList())));
        dto.setErrors(new ArrayList<>((List<String>) result.getOrDefault("errors",
                java.util.Collections.emptyList())));
        return dto;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> stringifyMap(Map<String, Object> map) {
        Map<String, String> result = new LinkedHashMap<>();
        if (map != null) {
            map.forEach((key, value) -> result.put(key, value == null ? null : String.valueOf(value)));
        }
        return result;
    }

    private Response error(Response.Status status, String message) {
        ErrorDTO errorDto = new ErrorDTO();
        errorDto.setCode((long) status.getStatusCode());
        errorDto.setMessage(message);
        return Response.status(status).entity(errorDto).build();
    }
}

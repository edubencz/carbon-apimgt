/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.apache.axis2.context.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * Inserted by {@link SequenceInstrumenter} directly after every {@code call blocking="true"}
 * mediator (never for non-blocking calls/sends), to resolve the pending outbound-call entry that
 * mediator's own {@link TraceMarkerMediator} enqueued, using the resulting {@code HTTP_SC}. If the
 * call was mocked, {@link MockResponderMediator} will already have resolved (and dequeued) that
 * entry by the time this runs, so this is then a harmless no-op. A non-blocking call's outcome
 * cannot be observed synchronously at any fixed point, so it gets no such mediator; its pending
 * entry is either resolved by {@link MockResponderMediator} (if mocked) or flushed
 * (nodeId/method/url only, no status) once the run finishes.
 */
public class OutboundCallMediator extends AbstractMediator {

    private String runId;
    private String nodeId;

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public boolean mediate(org.apache.synapse.MessageContext synCtx) {

        SandboxRunRegistry.RunRecord run = SandboxRunRegistry.get(runId);
        if (run == null) {
            return true;
        }
        MessageContext axis2MC = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        Object httpSc = axis2MC.getProperty(org.wso2.carbon.apimgt.gateway.APIMgtGatewayConstants.HTTP_SC);
        Integer status = null;
        if (httpSc != null) {
            try {
                status = Integer.valueOf(String.valueOf(httpSc));
            } catch (NumberFormatException ignored) {
                // leave status unset
            }
        }
        run.resolveOutboundCallByNodeId(nodeId, status);
        return true;
    }

    @Override
    public String getType() {
        return "APIMPolicySandboxOutboundCall";
    }
}

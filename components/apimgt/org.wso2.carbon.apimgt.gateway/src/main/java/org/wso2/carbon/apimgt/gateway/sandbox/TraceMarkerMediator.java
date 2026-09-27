/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * Class mediator inserted by {@link SequenceInstrumenter} immediately before every flow mediator of
 * the policy under test. Records {@code {order, nodeId, tag, tMs}} into the current run's
 * {@link SandboxRunRegistry.RunRecord} so the REST layer can return the contract's {@code trace}
 * array.
 *
 * <p>For a {@code call}/{@code send} mediator, {@link SequenceInstrumenter} also sets
 * {@code outboundMethod}/{@code outboundUrl}, which enqueues a pending
 * {@link SandboxRunRegistry.RunRecord#beginOutboundCall(String, String, String) outbound call} in
 * the same step - kept on this same mediator (rather than a second, separately-inserted "before"
 * marker) precisely so this stays the single mediator immediately preceding the instrumented
 * mediator; several tests, and the UI-side nodeId contract, rely on "the marker's next sibling is
 * the real mediator" holding for every nodeId.
 *
 * <pre>{@code
 * <class name="org.wso2.carbon.apimgt.gateway.sandbox.TraceMarkerMediator">
 *     <property name="runId" value="..."/>
 *     <property name="nodeId" value="3"/>
 *     <property name="tag" value="payloadFactory"/>
 * </class>
 * }</pre>
 */
public class TraceMarkerMediator extends AbstractMediator {

    private String runId;
    private String nodeId;
    private String tag;
    private String outboundMethod;
    private String outboundUrl;

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setOutboundMethod(String outboundMethod) {
        this.outboundMethod = outboundMethod;
    }

    public void setOutboundUrl(String outboundUrl) {
        this.outboundUrl = outboundUrl;
    }

    @Override
    public boolean mediate(MessageContext synCtx) {

        SandboxRunRegistry.RunRecord run = SandboxRunRegistry.get(runId);
        if (run != null) {
            run.addTrace(nodeId, tag);
            if (outboundUrl != null) {
                run.beginOutboundCall(nodeId, outboundMethod, outboundUrl);
            }
        }
        return true;
    }

    @Override
    public String getType() {
        return "APIMPolicySandboxTraceMarker";
    }
}

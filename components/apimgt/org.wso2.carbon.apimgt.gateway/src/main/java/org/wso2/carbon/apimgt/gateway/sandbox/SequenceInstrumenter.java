/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Adds trace markers before flow mediators. Paths follow the Publisher editor's
 * Editor/CONTRACT.md after the optional outer sequence has been unwrapped.
 */
public final class SequenceInstrumenter {

    private final String runId;

    public SequenceInstrumenter(String runId) {
        this.runId = runId;
    }

    public void instrument(OMElement container) {
        instrumentChildren(container, "");
    }

    private void instrumentChildren(OMElement container, String prefix) {
        List<OMElement> mediators = children(container);
        for (int index = 0; index < mediators.size(); index++) {
            OMElement mediator = mediators.get(index);
            String id = prefix.isEmpty() ? Integer.toString(index) : prefix + "/" + index;
            OMElement marker = addMarker(mediator, id);
            String name = mediator.getLocalName().toLowerCase(java.util.Locale.ROOT);
            switch (name) {
                case "log":
                    addLogReplica(mediator, id);
                    break;
                case "call":
                case "send":
                    addOutboundCallMarkers(mediator, id, marker);
                    break;
                case "filter":
                    OMElement thenBranch = child(mediator, "then");
                    OMElement elseBranch = child(mediator, "else");
                    if (thenBranch == null && elseBranch == null) {
                        instrumentChildren(mediator, id + "/then");
                    } else {
                        if (thenBranch != null) {
                            instrumentChildren(thenBranch, id + "/then");
                        }
                        if (elseBranch != null) {
                            instrumentChildren(elseBranch, id + "/else");
                        }
                    }
                    break;
                case "switch":
                    int caseIndex = 0;
                    for (OMElement branch : children(mediator)) {
                        if (is(branch, "case")) {
                            instrumentChildren(branch, id + "/case[" + caseIndex++ + "]");
                        } else if (is(branch, "default")) {
                            instrumentChildren(branch, id + "/default");
                        }
                    }
                    break;
                case "clone":
                    int targetIndex = 0;
                    for (OMElement target : children(mediator)) {
                        if (is(target, "target")) {
                            instrumentChildren(inlineSequence(target), id + "/target[" + targetIndex++ + "]");
                        }
                    }
                    break;
                case "iterate":
                case "foreach":
                    instrumentNamed(mediator, "target", id + "/target", true);
                    break;
                case "throttle":
                    instrumentNamed(mediator, "onAccept", id + "/onAccept", false);
                    instrumentNamed(mediator, "onReject", id + "/onReject", false);
                    break;
                case "cache":
                    instrumentNamed(mediator, "onCacheHit", id + "/onCacheHit", false);
                    break;
                case "aggregate":
                    instrumentNamed(mediator, "onComplete", id + "/onComplete", false);
                    break;
                case "validate":
                    instrumentNamed(mediator, "on-fail", id + "/on-fail", false);
                    break;
                default:
                    break;
            }
        }
    }

    private void instrumentNamed(OMElement parent, String childName, String prefix, boolean unwrap) {
        OMElement branch = child(parent, childName);
        if (branch != null) {
            instrumentChildren(unwrap ? inlineSequence(branch) : branch, prefix);
        }
    }

    private static OMElement inlineSequence(OMElement target) {
        List<OMElement> contents = children(target);
        return contents.size() == 1 && is(contents.get(0), "sequence") ? contents.get(0) : target;
    }

    private OMElement addMarker(OMElement mediator, String id) {
        OMElement marker = SandboxXmlUtil.classMediator(
                "org.wso2.carbon.apimgt.gateway.sandbox.TraceMarkerMediator", mediator.getOMFactory());
        marker.addChild(SandboxXmlUtil.classProperty("runId", runId, mediator.getOMFactory()));
        marker.addChild(SandboxXmlUtil.classProperty("nodeId", id, mediator.getOMFactory()));
        // Keep the element's original local name for the trace tag (e.g. "payloadFactory"), matching
        // what the Publisher editor's diagram displays. Lowercasing is still used, separately, for the
        // structural matching in instrumentChildren() below (filter/switch/etc. container detection);
        // it must not leak into the value the UI renders per trace entry.
        marker.addChild(SandboxXmlUtil.classProperty("tag", mediator.getLocalName(), mediator.getOMFactory()));
        ((OMNode) mediator).insertSiblingBefore(marker);
        return marker;
    }

    private void addLogReplica(OMElement log, String id) {
        StringBuilder spec = new StringBuilder();
        for (OMElement property : children(log)) {
            if (!is(property, "property")) {
                continue;
            }
            if (spec.length() > 0) {
                spec.append('\n');
            }
            String expression = attribute(property, "expression", null);
            spec.append(attribute(property, "name", "")).append('\t');
            if (expression == null) {
                spec.append('L').append('\t').append(attribute(property, "value", ""));
            } else {
                spec.append('X').append('\t').append(expression);
            }
        }
        OMElement replica = SandboxXmlUtil.classMediator(
                "org.wso2.carbon.apimgt.gateway.sandbox.LogReplicaMediator", log.getOMFactory());
        replica.addChild(SandboxXmlUtil.classProperty("runId", runId, log.getOMFactory()));
        replica.addChild(SandboxXmlUtil.classProperty("nodeId", id, log.getOMFactory()));
        replica.addChild(SandboxXmlUtil.classProperty("level", attribute(log, "level", "INFO"), log.getOMFactory()));
        replica.addChild(SandboxXmlUtil.classProperty("spec", spec.toString(), log.getOMFactory()));
        ((OMNode) log).insertSiblingAfter(replica);
    }

    /**
     * Adds {@code outboundMethod}/{@code outboundUrl} properties to the {@code call}/{@code send}
     * mediator's own {@link TraceMarkerMediator} (the intended method/URL from its inline
     * {@code endpoint/http@uri-template} or {@code endpoint/address@uri} - a registry-key or
     * dynamic endpoint just yields no outbound tracking for that node) so it enqueues a pending
     * outbound call in {@link SandboxRunRegistry} when it fires (see that mediator's javadoc for
     * why this rides on the existing marker instead of a separately-inserted one). For the
     * {@code blocking="true"} shape only, also inserts an {@link OutboundCallMediator} AFTER the
     * call to resolve that pending entry with the resulting {@code HTTP_SC} (mediation for a
     * non-blocking call continues asynchronously past this point, so its outcome cannot be
     * captured synchronously here - see {@code MockResponderMediator}, which resolves mocked calls
     * itself regardless of blocking/non-blocking).
     */
    private void addOutboundCallMarkers(OMElement callMediator, String id, OMElement marker) {
        OMElement endpoint = child(callMediator, "endpoint");
        String method = null;
        String url = null;
        if (endpoint != null) {
            OMElement http = child(endpoint, "http");
            OMElement address = child(endpoint, "address");
            if (http != null) {
                method = attribute(http, "method", "POST");
                url = attribute(http, "uri-template", null);
            } else if (address != null) {
                url = attribute(address, "uri", null);
            }
        }
        if (url == null) {
            return;
        }
        marker.addChild(SandboxXmlUtil.classProperty("outboundMethod", method == null ? "POST" : method,
                callMediator.getOMFactory()));
        marker.addChild(SandboxXmlUtil.classProperty("outboundUrl", url, callMediator.getOMFactory()));

        boolean blocking = "true".equalsIgnoreCase(attribute(callMediator, "blocking", "false"));
        if (blocking) {
            OMElement after = SandboxXmlUtil.classMediator(
                    "org.wso2.carbon.apimgt.gateway.sandbox.OutboundCallMediator", callMediator.getOMFactory());
            after.addChild(SandboxXmlUtil.classProperty("runId", runId, callMediator.getOMFactory()));
            after.addChild(SandboxXmlUtil.classProperty("nodeId", id, callMediator.getOMFactory()));
            ((OMNode) callMediator).insertSiblingAfter(after);
        }
    }

    private static String attribute(OMElement element, String name, String defaultValue) {
        String value = element.getAttributeValue(new QName(name));
        return value == null ? defaultValue : value;
    }

    private static boolean is(OMElement element, String name) {
        return name.equalsIgnoreCase(element.getLocalName());
    }

    private static OMElement child(OMElement parent, String name) {
        for (OMElement element : children(parent)) {
            if (is(element, name)) {
                return element;
            }
        }
        return null;
    }

    private static List<OMElement> children(OMElement parent) {
        List<OMElement> result = new ArrayList<>();
        Iterator<OMElement> iterator = parent.getChildElements();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }
}

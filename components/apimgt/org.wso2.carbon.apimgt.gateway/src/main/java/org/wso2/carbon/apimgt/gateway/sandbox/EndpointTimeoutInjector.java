/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.apache.axiom.om.OMElement;

import java.util.Iterator;
import java.util.Locale;

/**
 * Adds a {@code <timeout><duration>...</duration><responseAction>fault</responseAction></timeout>}
 * to every inline {@code http}/{@code address} endpoint that does not already declare one, so a
 * real (non-mocked) outbound call can never hang past the run's {@code execution_timeout} and turn
 * an otherwise-bounded sandbox run into an unbounded one.
 */
public final class EndpointTimeoutInjector {

    private EndpointTimeoutInjector() {
    }

    public static void inject(OMElement root, long timeoutMs) {
        Iterator<OMElement> children = root.getChildElements();
        while (children.hasNext()) {
            OMElement child = children.next();
            String localName = child.getLocalName().toLowerCase(Locale.ROOT);
            if ("http".equals(localName) || "address".equals(localName)) {
                ensureTimeout(child, timeoutMs);
            }
            inject(child, timeoutMs);
        }
    }

    private static void ensureTimeout(OMElement endpointTarget, long timeoutMs) {
        Iterator<OMElement> children = endpointTarget.getChildElements();
        while (children.hasNext()) {
            if ("timeout".equalsIgnoreCase(children.next().getLocalName())) {
                return;
            }
        }
        org.apache.axiom.om.OMFactory factory = endpointTarget.getOMFactory();
        org.apache.axiom.om.OMNamespace ns = factory.createOMNamespace(SandboxXmlUtil.SYNAPSE_NS, "");
        OMElement timeout = factory.createOMElement("timeout", ns);
        OMElement duration = factory.createOMElement("duration", ns);
        duration.setText(String.valueOf(timeoutMs));
        OMElement responseAction = factory.createOMElement("responseAction", ns);
        responseAction.setText("fault");
        timeout.addChild(duration);
        timeout.addChild(responseAction);
        endpointTarget.addChild(timeout);
    }
}

/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;

/**
 * Small Axiom helpers for building the &lt;class&gt; mediator fragments the sandbox instrumenter
 * and orchestrator weave into the temporary sandbox API. (Formerly {@code SpikeXml}, Phase 1.)
 */
public final class SandboxXmlUtil {

    public static final String SYNAPSE_NS = "http://ws.apache.org/ns/synapse";

    /**
     * The per-run guard credential header (see {@link SandboxGuardMediator}). It must never reach
     * the REST response: {@code SandboxGuardMediator} only reads it (it must not structurally
     * mutate the live {@code TRANSPORT_HEADERS} map - see its javadoc), and {@code SandboxApiBuilder}
     * removes it via a plain "remove" property mediator (scope="transport") right after the guard
     * filter in the inSequence, and again at the top of the out/fault sequences, so it cannot be
     * echoed back by a later {@code <respond/>} or forwarded to a mock endpoint. {@link
     * CaptureMediator} and {@code PolicySandboxService} additionally exclude it defensively
     * wherever transport headers are captured or surfaced.
     */
    public static final String SANDBOX_TOKEN_HEADER = "X-APIM-Sandbox-Token";

    private SandboxXmlUtil() {
    }

    public static OMElement classMediator(String className, OMFactory factory) {
        OMFactory f = factory != null ? factory : OMAbstractFactory.getOMFactory();
        OMNamespace ns = f.createOMNamespace(SYNAPSE_NS, "");
        OMElement classEl = f.createOMElement("class", ns);
        classEl.addAttribute("name", className, null);
        return classEl;
    }

    public static OMElement classMediator(String className) {
        return classMediator(className, null);
    }

    public static OMElement classProperty(String name, String value, OMFactory factory) {
        OMFactory f = factory != null ? factory : OMAbstractFactory.getOMFactory();
        OMNamespace ns = f.createOMNamespace(SYNAPSE_NS, "");
        OMElement property = f.createOMElement("property", ns);
        property.addAttribute("name", name, null);
        property.addAttribute("value", value == null ? "" : value, null);
        return property;
    }

    public static String escapeXmlText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static String escapeXmlAttribute(String value) {
        if (value == null) {
            return "";
        }
        return escapeXmlText(value).replace("\"", "&quot;");
    }
}

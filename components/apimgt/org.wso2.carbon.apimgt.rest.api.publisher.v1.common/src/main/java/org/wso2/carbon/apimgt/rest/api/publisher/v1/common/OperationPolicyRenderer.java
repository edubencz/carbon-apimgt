/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package org.wso2.carbon.apimgt.rest.api.publisher.v1.common;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.RenderResult;
import com.hubspot.jinjava.interpret.TemplateError;
import org.apache.axiom.om.OMElement;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Renders a policy with the same Jinjava engine as SynapsePolicyAggregator and validates its XML. */
public final class OperationPolicyRenderer {

    private static final String SYNAPSE_NS = "http://ws.apache.org/ns/synapse";
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)");

    public RenderOutput render(String policyDefinition, Map<String, Object> attributes) {
        RenderOutput output = new RenderOutput();
        if (policyDefinition == null || policyDefinition.trim().isEmpty()) {
            output.errors.add(new Marker(1, 1, "Policy definition is empty", "error"));
            return output;
        }
        Matcher variableMatcher = VARIABLE.matcher(policyDefinition);
        Set<String> variables = new LinkedHashSet<>();
        while (variableMatcher.find()) {
            variables.add(variableMatcher.group(1));
        }
        output.detectedVariables.addAll(variables);

        RenderResult rendered = new Jinjava().renderForResult(policyDefinition,
                attributes == null ? Collections.emptyMap() : attributes);
        for (TemplateError error : rendered.getErrors()) {
            output.errors.add(new Marker(Math.max(1, error.getLineno()),
                    Math.max(1, error.getStartPosition()), error.getMessage(),
                    error.getSeverity() == TemplateError.ErrorType.WARNING ? "warning" : "error"));
        }
        if (rendered.getOutput() == null) {
            return output;
        }
        output.renderedSequence = rendered.getOutput();
        try {
            // A synthetic root accepts policy fragments with multiple top-level mediators.
            // APIUtil configures StAX with DTD and external entities disabled.
            String wrapped = "<root xmlns=\"" + SYNAPSE_NS + "\">" + output.renderedSequence + "</root>";
            OMElement root = APIUtil.buildSecuredOMElement(
                    new ByteArrayInputStream(wrapped.getBytes(StandardCharsets.UTF_8)));
            root.build();
            List<OMElement> children = elements(root);
            OMElement container = root;
            if (children.size() == 1 && "sequence".equalsIgnoreCase(children.get(0).getLocalName())) {
                container = children.get(0);
                output.normalized = true;
                output.warnings.add("Outer <sequence> was removed before policy execution");
            }
            StringBuilder body = new StringBuilder();
            for (OMElement child : elements(container)) {
                body.append(child.toString());
            }
            output.renderedSequence = body.toString();
        } catch (APIManagementException | RuntimeException e) {
            XMLStreamException xmlError = findXmlError(e);
            int line = xmlError == null || xmlError.getLocation() == null ? 1
                    : Math.max(1, xmlError.getLocation().getLineNumber());
            int column = xmlError == null || xmlError.getLocation() == null ? 1
                    : Math.max(1, xmlError.getLocation().getColumnNumber());
            output.errors.add(new Marker(line, column, e.getMessage(), "error"));
        }
        return output;
    }

    private static XMLStreamException findXmlError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof XMLStreamException) {
                return (XMLStreamException) current;
            }
            current = current.getCause();
        }
        return null;
    }

    private static List<OMElement> elements(OMElement parent) {
        List<OMElement> children = new ArrayList<>();
        Iterator<OMElement> iterator = parent.getChildElements();
        while (iterator.hasNext()) {
            children.add(iterator.next());
        }
        return children;
    }

    public static final class RenderOutput {
        public String renderedSequence;
        public boolean normalized;
        public final List<Marker> errors = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
        public final List<String> detectedVariables = new ArrayList<>();
        public final List<String> unknownMediators = new ArrayList<>();
    }

    public static final class Marker {
        public final int line;
        public final int column;
        public final String message;
        public final String severity;

        public Marker(int line, int column, String message, String severity) {
            this.line = line;
            this.column = column;
            this.message = message;
            this.severity = severity;
        }
    }
}

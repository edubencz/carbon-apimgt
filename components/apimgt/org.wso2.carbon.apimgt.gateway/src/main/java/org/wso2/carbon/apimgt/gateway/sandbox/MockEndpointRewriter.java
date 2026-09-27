/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Rewrites {@code call}/{@code send} endpoints whose rendered target URL matches a configured mock
 * (GLOB or REGEX on the URL, optionally also on the method) to the per-run mock API's loopback URL.
 * Endpoints referenced by registry {@code key}, a dynamic {@code To} header, or a
 * {@code key-expression} cannot be rewritten (their real target is not known statically); those
 * just get a warning instead. Run AFTER {@link SequenceInstrumenter}: the trace/outbound-call
 * markers it inserts capture the ORIGINAL, pre-rewrite URL, which is what the sandbox contract's
 * {@code outboundCalls[].url} is meant to show.
 */
public final class MockEndpointRewriter {

    private MockEndpointRewriter() {
    }

    /**
     * @param root      the (already instrumented) sequence root to rewrite in place.
     * @param mocks     the run's configured mocks (id/urlPattern/matchType/method).
     * @param mockBaseUrl the per-run mock API's base loopback URL, e.g.
     *                  {@code http://127.0.0.1:8281/__apim_policy_sandbox/<runId>/mocks}.
     * @param warnings  collector for "cannot be mocked" warnings.
     */
    public static void rewrite(OMElement root, List<Mock> mocks, String mockBaseUrl, List<String> warnings) {
        if (mocks == null || mocks.isEmpty()) {
            return;
        }
        rewriteRecursively(root, mocks, mockBaseUrl, warnings);
    }

    private static void rewriteRecursively(OMElement element, List<Mock> mocks, String mockBaseUrl,
                                            List<String> warnings) {
        Iterator<OMElement> children = element.getChildElements();
        while (children.hasNext()) {
            OMElement child = children.next();
            String localName = child.getLocalName().toLowerCase(Locale.ROOT);
            if ("call".equals(localName) || "send".equals(localName)) {
                rewriteCall(child, mocks, mockBaseUrl, warnings);
            }
            rewriteRecursively(child, mocks, mockBaseUrl, warnings);
        }
    }

    private static void rewriteCall(OMElement callMediator, List<Mock> mocks, String mockBaseUrl,
                                     List<String> warnings) {
        OMElement endpoint = firstChild(callMediator, "endpoint");
        if (endpoint == null) {
            return;
        }
        if (firstChild(endpoint, "http") == null && firstChild(endpoint, "address") == null) {
            String key = endpoint.getAttributeValue(new QName("key"));
            if (key != null) {
                warnings.add("endpoint key='" + key + "' cannot be mocked (registry-referenced endpoint)");
            } else {
                warnings.add("dynamic endpoint (e.g. 'To' header or key-expression) cannot be mocked");
            }
            return;
        }
        OMElement http = firstChild(endpoint, "http");
        OMElement address = firstChild(endpoint, "address");
        OMElement target = http != null ? http : address;
        QName uriAttr = new QName(http != null ? "uri-template" : "uri");
        String url = target.getAttributeValue(uriAttr);
        if (url == null) {
            return;
        }
        String method = http != null ? target.getAttributeValue(new QName("method")) : null;
        Mock match = findMatch(mocks, url, method);
        if (match == null) {
            return;
        }
        target.getAttribute(uriAttr).setAttributeValue(mockBaseUrl + "/" + match.id);
    }

    private static Mock findMatch(List<Mock> mocks, String url, String method) {
        String normalizedMethod = (method == null || method.trim().isEmpty()) ? null : method;
        for (Mock mock : mocks) {
            if (mock.method != null && normalizedMethod != null && !mock.method.equalsIgnoreCase(normalizedMethod)) {
                continue;
            }
            if (matches(mock, url)) {
                return mock;
            }
        }
        return null;
    }

    private static boolean matches(Mock mock, String url) {
        try {
            if ("REGEX".equalsIgnoreCase(mock.matchType)) {
                return Pattern.matches(mock.urlPattern, url);
            }
            return Pattern.matches(globToRegex(mock.urlPattern), url);
        } catch (Exception e) {
            return false;
        }
    }

    private static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append('.');
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '\\':
                case '[':
                case ']':
                case '{':
                case '}':
                    regex.append('\\').append(c);
                    break;
                default:
                    regex.append(c);
            }
        }
        return regex.toString();
    }

    private static OMElement firstChild(OMElement parent, String localName) {
        Iterator<OMElement> children = parent.getChildElements();
        while (children.hasNext()) {
            OMElement child = children.next();
            if (localName.equalsIgnoreCase(child.getLocalName())) {
                return child;
            }
        }
        return null;
    }

    /** Plain data holder mirroring the contract's {@code PolicySandboxMock} shape. */
    public static final class Mock {
        public final String id;
        public final String urlPattern;
        public final String matchType;
        public final String method;

        public Mock(String id, String urlPattern, String matchType, String method) {
            this.id = id;
            this.urlPattern = urlPattern;
            this.matchType = matchType;
            this.method = (method == null || method.trim().isEmpty()) ? null : method;
        }
    }
}

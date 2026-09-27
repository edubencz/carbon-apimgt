package org.wso2.carbon.apimgt.gateway.sandbox;

import org.apache.axis2.context.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/** Rejects requests that do not come from the sandbox's own loopback client. */
public class SandboxGuardMediator extends AbstractMediator {

    private static final Log LOG = LogFactory.getLog(SandboxGuardMediator.class);

    private String token;

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean mediate(org.apache.synapse.MessageContext context) {
        MessageContext axis2 = ((Axis2MessageContext) context).getAxis2MessageContext();
        String remote = (String) axis2.getProperty(MessageContext.REMOTE_ADDR);
        @SuppressWarnings("unchecked")
        Map<String, Object> headers = (Map<String, Object>) axis2.getProperty(MessageContext.TRANSPORT_HEADERS);
        String supplied = null;
        if (headers != null) {
            // Only read the token here - do NOT structurally mutate the live TRANSPORT_HEADERS map.
            // That map is not a private copy: the passthrough transport (PTT) can reuse the same
            // instance as the basis for the response's transport headers, and/or for the outbound
            // request headers of a later <call blocking="true"/>, so an in-place iterator.remove()
            // here left the map (and whatever PTT-internal header state mirrors it) in a shape the
            // transport did not expect - in practice this caused the fault-path <respond/> to never
            // reach the loopback client at all (the run hung until execution_timeout instead of
            // returning the fault promptly). Actually removing the header from TRANSPORT_HEADERS is
            // done later via a plain "remove" property mediator (scope="transport"), which is the
            // PTT-safe way to do it; see SandboxApiBuilder. CaptureMediator and PolicySandboxService
            // additionally filter this header out wherever headers are captured or surfaced, as
            // defense in depth.
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                if (SandboxXmlUtil.SANDBOX_TOKEN_HEADER.equalsIgnoreCase(header.getKey())) {
                    supplied = String.valueOf(header.getValue());
                    break;
                }
            }
        }
        boolean authorized = isLoopback(remote) && tokenMatches(supplied);
        context.setProperty("APIM_SANDBOX_AUTHORIZED", authorized);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sandbox guard evaluated: remote=" + remote + ", authorized=" + authorized);
        }
        if (!authorized) {
            LOG.warn("Sandbox request rejected: remote=" + remote + ", tokenPresent=" + (supplied != null)
                    + ", expectedTokenConfigured=" + (token != null));
        }
        return true;
    }

    @Override
    public String getType() {
        return "APIMPolicySandboxGuard";
    }

    private boolean tokenMatches(String supplied) {
        return token != null && supplied != null && MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@code MessageContext.REMOTE_ADDR} on this transport is observed (see
     * {@code GatewayUtils.getClientIp}, same product convention) to carry a trailing
     * {@code ":port"} for plain IPv4 loopback callers (e.g. {@code "127.0.0.1:54321"}), which makes
     * a naive {@code InetAddress.getByName(remote)} throw {@link UnknownHostException} and this
     * guard fail closed (always 403) even for a legitimate same-host caller. Strip a trailing port
     * only for the unambiguous IPv4-with-port shape (exactly one colon); IPv6 loopback forms such as
     * {@code "::1"} or the expanded {@code "0:0:0:0:0:0:0:1"} contain multiple colons and are passed
     * through unchanged, since {@link InetAddress#getByName(String)} parses those directly (and
     * accepts an optional {@code "[::1]:port"} bracketed form, also handled below).
     */
    private static boolean isLoopback(String remote) {
        if (remote == null || remote.isEmpty()) {
            return false;
        }
        String host = remote.trim();
        if (host.startsWith("[")) {
            int close = host.indexOf(']');
            if (close > -1) {
                host = host.substring(1, close);
            }
        } else if (host.indexOf(':') > -1 && host.split(":").length == 2) {
            host = host.substring(0, host.indexOf(':'));
        }
        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (UnknownHostException e) {
            LOG.warn("Sandbox guard could not parse remote address '" + remote + "'", e);
            return false;
        }
    }
}

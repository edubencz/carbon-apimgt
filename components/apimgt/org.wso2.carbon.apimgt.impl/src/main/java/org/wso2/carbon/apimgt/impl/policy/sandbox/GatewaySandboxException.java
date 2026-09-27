/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com/).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.impl.policy.sandbox;

/**
 * Thrown by {@link GatewaySandboxClient} when the gateway policy sandbox execution could not be
 * completed. The {@link Reason} lets the REST layer map this to the HTTP status defined by the
 * Control Plane &lt;-&gt; Gateway sandbox contract.
 */
public class GatewaySandboxException extends Exception {

    private static final long serialVersionUID = 1L;

    /** Why the gateway sandbox execution failed. */
    public enum Reason {
        /** The named gateway environment does not exist or has no server/sandbox URL. */
        ENVIRONMENT_NOT_FOUND,
        /** The gateway responded 404: {@code [apim.policy_sandbox] enable} is false there. */
        DISABLED,
        /** The gateway responded 429: too many concurrent sandbox runs. */
        TOO_MANY_RUNS,
        /** The gateway responded 413: request body exceeds the gateway's size limit. */
        PAYLOAD_TOO_LARGE,
        /** The gateway responded 400: invalid sandbox request (e.g. invalid rendered XML). */
        BAD_REQUEST,
        /** The gateway could not be reached (connection refused/timeout/DNS/TLS failure). */
        UNREACHABLE,
        /** The gateway responded with an unexpected status code. */
        GATEWAY_ERROR
    }

    private final Reason reason;
    private final String gatewayResponseBody;

    public GatewaySandboxException(Reason reason, String message, String gatewayResponseBody) {
        super(message);
        this.reason = reason;
        this.gatewayResponseBody = gatewayResponseBody;
    }

    public GatewaySandboxException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.gatewayResponseBody = null;
    }

    public Reason getReason() {
        return reason;
    }

    /** The raw gateway response body, if any was received (e.g. the {@code errors} array on a 400). */
    public String getGatewayResponseBody() {
        return gatewayResponseBody;
    }
}

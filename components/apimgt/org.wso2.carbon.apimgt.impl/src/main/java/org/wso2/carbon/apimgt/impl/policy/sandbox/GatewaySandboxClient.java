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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.Environment;
import org.wso2.carbon.apimgt.impl.dto.OperationPolicySandboxConfig;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls the Gateway's {@code POST /policy-sandbox/execute} endpoint on behalf of the Control Plane's
 * {@code POST /operation-policies/test} REST resource, as defined by the Control Plane &lt;-&gt; Gateway
 * policy sandbox contract (see {@code dev-scripts/sandbox-contract.md}).
 *
 * <p>The base URL of a gateway environment's sandbox endpoint is resolved as follows:
 * <ol>
 *   <li>the environment's optional {@code sandboxURL}, when configured; otherwise</li>
 *   <li>the environment's {@code serverURL}, with a trailing {@code /services/} replaced by
 *       {@code /api/am/gateway/v2}.</li>
 * </ol>
 *
 * <p>This class lives in {@code org.wso2.carbon.apimgt.impl} (rather than in the publisher REST module)
 * so that it only depends on {@code APIManagerConfiguration}/{@code Environment}, which already live here,
 * and so any other Control Plane caller (e.g. a future admin/devops tool) can reuse it without pulling in
 * the publisher REST API module.
 */
public class GatewaySandboxClient {

    private static final Log log = LogFactory.getLog(GatewaySandboxClient.class);

    private static final String SANDBOX_EXECUTE_PATH = "/policy-sandbox/execute";
    private static final String SERVICES_SUFFIX = "/services/";
    private static final String GATEWAY_V2_SUFFIX = "/api/am/gateway/v2";

    /**
     * Executes an already-rendered policy sequence on the named gateway environment's sandbox endpoint.
     *
     * @param environmentName name of a config-defined ({@code api-manager.xml} / {@code deployment.toml})
     *                         gateway environment
     * @param requestJson     the {@code PolicySandboxExecuteRequest} JSON payload, already built by the caller
     * @return the gateway's {@code PolicySandboxExecuteResponse}, parsed as a free-form JSON object
     * @throws GatewaySandboxException on any failure; see {@link GatewaySandboxException.Reason}
     */
    public JsonObject execute(String environmentName, String requestJson) throws GatewaySandboxException {

        Environment environment = resolveEnvironment(environmentName);
        String baseUrl = resolveBaseUrl(environment);
        String url = baseUrl + SANDBOX_EXECUTE_PATH;
        OperationPolicySandboxConfig config = ServiceReferenceHolder.getInstance()
                .getAPIManagerConfigurationService().getAPIManagerConfiguration().getOperationPolicySandboxConfig();

        HttpPost post = new HttpPost(url);
        int timeout = config.getTimeout();
        // Uses the legacy HttpParams timeout API (stable across the multiple httpclient versions that
        // coexist on this module's classpath) rather than RequestConfig#setConfig, which is only present
        // on HttpRequestBase from httpclient 4.3 onward.
        HttpConnectionParams.setConnectionTimeout(post.getParams(), timeout);
        HttpConnectionParams.setSoTimeout(post.getParams(), timeout);
        post.setHeader(HttpHeaders.AUTHORIZATION, basicAuthHeader(environment));
        post.setHeader(HttpHeaders.ACCEPT, "application/json");
        post.setEntity(new StringEntity(requestJson, ContentType.APPLICATION_JSON));

        HttpClient client;
        try {
            client = APIUtil.getHttpClient(url);
        } catch (APIManagementException e) {
            throw new GatewaySandboxException(GatewaySandboxException.Reason.UNREACHABLE,
                    "Could not create an HTTP client for gateway environment '" + environmentName + "': "
                            + e.getMessage(), e);
        }

        String responseBody;
        int status;
        try {
            HttpResponse response = client.execute(post);
            status = response.getStatusLine().getStatusCode();
            responseBody = response.getEntity() != null
                    ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            throw new GatewaySandboxException(GatewaySandboxException.Reason.UNREACHABLE,
                    "Could not reach the gateway sandbox endpoint at " + url + ": " + e.getMessage(), e);
        }

        return handleResponse(status, responseBody, environmentName, url);
    }

    /**
     * Maps a gateway HTTP response to either the parsed {@code PolicySandboxExecuteResponse} (on 200) or a
     * {@link GatewaySandboxException} with the {@link GatewaySandboxException.Reason} defined by the
     * Control Plane &lt;-&gt; Gateway sandbox contract for 400/404/413/429 and any other status. Extracted
     * from {@link #execute(String, String)} so the status-code mapping can be unit tested without an actual
     * HTTP round trip.
     */
    static JsonObject handleResponse(int status, String responseBody, String environmentName, String url)
            throws GatewaySandboxException {
        switch (status) {
            case 200:
                return parseJsonObject(responseBody, url);
            case 404:
                throw new GatewaySandboxException(GatewaySandboxException.Reason.DISABLED,
                        "The policy sandbox is disabled on gateway environment '" + environmentName + "'",
                        responseBody);
            case 429:
                throw new GatewaySandboxException(GatewaySandboxException.Reason.TOO_MANY_RUNS,
                        "The gateway rejected the sandbox run: too many concurrent runs", responseBody);
            case 413:
                throw new GatewaySandboxException(GatewaySandboxException.Reason.PAYLOAD_TOO_LARGE,
                        "The gateway rejected the sandbox run: request body too large", responseBody);
            case 400:
                throw new GatewaySandboxException(GatewaySandboxException.Reason.BAD_REQUEST,
                        "The gateway rejected the sandbox request as invalid", responseBody);
            default:
                throw new GatewaySandboxException(GatewaySandboxException.Reason.GATEWAY_ERROR,
                        "Unexpected gateway response: HTTP " + status, responseBody);
        }
    }

    /**
     * Lists the config-defined gateway environments that can be used as a sandbox target, i.e. those with
     * either a {@code sandboxURL} or a {@code serverURL}. Environments created only via the Admin portal
     * (persisted in the database) are not config-defined and are therefore excluded.
     */
    public Map<String, Environment> getSandboxCapableEnvironments() {
        Map<String, Environment> result = new LinkedHashMap<>();
        for (Map.Entry<String, Environment> entry : APIUtil.getReadOnlyEnvironments().entrySet()) {
            if (hasSandboxCapableUrl(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    static boolean hasSandboxCapableUrl(Environment environment) {
        return StringUtils.isNotBlank(environment.getSandboxURL()) || StringUtils.isNotBlank(
                environment.getServerURL());
    }

    Environment resolveEnvironment(String environmentName) throws GatewaySandboxException {
        if (StringUtils.isBlank(environmentName)) {
            throw new GatewaySandboxException(GatewaySandboxException.Reason.ENVIRONMENT_NOT_FOUND,
                    "A gateway environment name is required", (String) null);
        }
        Environment environment = APIUtil.getReadOnlyEnvironments().get(environmentName);
        if (environment == null || !hasSandboxCapableUrl(environment)) {
            throw new GatewaySandboxException(GatewaySandboxException.Reason.ENVIRONMENT_NOT_FOUND,
                    "Gateway environment '" + environmentName + "' was not found, or has no server/sandbox URL "
                            + "configured", (String) null);
        }
        return environment;
    }

    static String resolveBaseUrl(Environment environment) {
        String sandboxUrl = environment.getSandboxURL();
        if (StringUtils.isNotBlank(sandboxUrl)) {
            return StringUtils.removeEnd(sandboxUrl.trim(), "/");
        }
        String serverUrl = StringUtils.trimToEmpty(environment.getServerURL());
        if (serverUrl.endsWith(SERVICES_SUFFIX)) {
            return StringUtils.removeEnd(serverUrl, SERVICES_SUFFIX) + GATEWAY_V2_SUFFIX;
        }
        // Best effort fallback for a serverURL that does not end with the conventional "/services/".
        return StringUtils.removeEnd(serverUrl, "/") + GATEWAY_V2_SUFFIX;
    }

    private static String basicAuthHeader(Environment environment) {
        String credentials = environment.getUserName() + ":" + environment.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private static JsonObject parseJsonObject(String body, String url) throws GatewaySandboxException {
        try {
            JsonElement element = JsonParser.parseString(body);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        } catch (JsonSyntaxException e) {
            log.debug("Gateway sandbox response from " + url + " was not valid JSON", e);
        }
        throw new GatewaySandboxException(GatewaySandboxException.Reason.GATEWAY_ERROR,
                "The gateway returned a 200 response that was not a JSON object", body);
    }
}

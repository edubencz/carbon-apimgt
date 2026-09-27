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

import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.api.model.Environment;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for {@link GatewaySandboxClient}: sandbox base URL derivation (per the Control Plane
 * &lt;-&gt; Gateway sandbox contract), environment resolution, and the gateway HTTP status -&gt;
 * {@link GatewaySandboxException.Reason} mapping. The mapping is tested via the extracted
 * {@link GatewaySandboxClient#handleResponse} rather than a live HTTP round trip, since this module's
 * mixed httpclient versions make mocking {@code APIUtil.getHttpClient} under PowerMock brittle; the real
 * end-to-end call is exercised by the runtime verification in Task F.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(APIUtil.class)
public class GatewaySandboxClientTest {

    // --- resolveBaseUrl ---

    @Test
    public void usesSandboxUrlWhenConfigured() {
        Environment environment = new Environment();
        environment.setServerURL("https://gw:9443/services/");
        environment.setSandboxURL("https://gw:9444/api/am/gateway/v2/");
        Assert.assertEquals("https://gw:9444/api/am/gateway/v2",
                GatewaySandboxClient.resolveBaseUrl(environment));
    }

    @Test
    public void derivesFromServerUrlServicesSuffix() {
        Environment environment = new Environment();
        environment.setServerURL("https://localhost:9443/services/");
        Assert.assertEquals("https://localhost:9443/api/am/gateway/v2",
                GatewaySandboxClient.resolveBaseUrl(environment));
    }

    @Test
    public void fallsBackWhenServerUrlHasNoServicesSuffix() {
        Environment environment = new Environment();
        environment.setServerURL("https://localhost:9443/");
        Assert.assertEquals("https://localhost:9443/api/am/gateway/v2",
                GatewaySandboxClient.resolveBaseUrl(environment));
    }

    // --- resolveEnvironment ---

    @Test
    public void resolveEnvironmentFindsSandboxCapableEnvironment() throws Exception {
        Environment environment = new Environment();
        environment.setName("Default");
        environment.setServerURL("https://localhost:9443/services/");
        mockReadOnlyEnvironments(environment);

        Environment resolved = new GatewaySandboxClient().resolveEnvironment("Default");
        Assert.assertSame(environment, resolved);
    }

    @Test
    public void resolveEnvironmentFailsWhenNotFound() throws Exception {
        mockReadOnlyEnvironments();
        try {
            new GatewaySandboxClient().resolveEnvironment("Missing");
            Assert.fail("Expected GatewaySandboxException");
        } catch (GatewaySandboxException e) {
            Assert.assertEquals(GatewaySandboxException.Reason.ENVIRONMENT_NOT_FOUND, e.getReason());
        }
    }

    @Test
    public void resolveEnvironmentFailsWhenNoUrlConfigured() throws Exception {
        Environment environment = new Environment();
        environment.setName("NoUrl");
        mockReadOnlyEnvironments(environment);
        try {
            new GatewaySandboxClient().resolveEnvironment("NoUrl");
            Assert.fail("Expected GatewaySandboxException");
        } catch (GatewaySandboxException e) {
            Assert.assertEquals(GatewaySandboxException.Reason.ENVIRONMENT_NOT_FOUND, e.getReason());
        }
    }

    // --- handleResponse: gateway HTTP status -> Reason mapping, per the sandbox contract ---

    @Test
    public void handleResponse200ReturnsParsedJson() throws Exception {
        JsonObject result = GatewaySandboxClient.handleResponse(200, "{\"status\":\"COMPLETED\"}", "Default",
                "http://gw/policy-sandbox/execute");
        Assert.assertEquals("COMPLETED", result.get("status").getAsString());
    }

    @Test
    public void handleResponse404MapsToDisabled() {
        assertReason(404, GatewaySandboxException.Reason.DISABLED);
    }

    @Test
    public void handleResponse429MapsToTooManyRuns() {
        assertReason(429, GatewaySandboxException.Reason.TOO_MANY_RUNS);
    }

    @Test
    public void handleResponse413MapsToPayloadTooLarge() {
        assertReason(413, GatewaySandboxException.Reason.PAYLOAD_TOO_LARGE);
    }

    @Test
    public void handleResponse400MapsToBadRequest() {
        assertReason(400, GatewaySandboxException.Reason.BAD_REQUEST);
    }

    @Test
    public void handleResponse500MapsToGatewayError() {
        assertReason(500, GatewaySandboxException.Reason.GATEWAY_ERROR);
    }

    @Test
    public void handleResponse200WithNonJsonBodyIsGatewayError() {
        try {
            GatewaySandboxClient.handleResponse(200, "not json", "Default", "http://gw/policy-sandbox/execute");
            Assert.fail("Expected GatewaySandboxException");
        } catch (GatewaySandboxException e) {
            Assert.assertEquals(GatewaySandboxException.Reason.GATEWAY_ERROR, e.getReason());
        }
    }

    private static void assertReason(int status, GatewaySandboxException.Reason expectedReason) {
        try {
            GatewaySandboxClient.handleResponse(status, "{\"code\":" + status + "}", "Default",
                    "http://gw/policy-sandbox/execute");
            Assert.fail("Expected GatewaySandboxException for status " + status);
        } catch (GatewaySandboxException e) {
            Assert.assertEquals(expectedReason, e.getReason());
            Assert.assertEquals("{\"code\":" + status + "}", e.getGatewayResponseBody());
        }
    }

    private static void mockReadOnlyEnvironments(Environment... environments) {
        Map<String, Environment> map = new HashMap<>();
        for (Environment environment : environments) {
            map.put(environment.getName(), environment);
        }
        PowerMockito.mockStatic(APIUtil.class);
        Mockito.when(APIUtil.getReadOnlyEnvironments()).thenReturn(map);
    }
}

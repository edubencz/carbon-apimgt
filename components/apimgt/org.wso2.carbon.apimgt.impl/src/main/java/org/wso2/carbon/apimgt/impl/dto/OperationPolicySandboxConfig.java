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

package org.wso2.carbon.apimgt.impl.dto;

import org.wso2.carbon.apimgt.impl.APIConstants;

import java.io.Serializable;

/**
 * Holds the Control Plane configuration for the operation policy sandbox ("Testar agora") feature,
 * parsed from the {@code <OperationPolicySandbox>} block of api-manager.xml.
 */
public class OperationPolicySandboxConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean enabled = APIConstants.OPERATION_POLICY_SANDBOX_ENABLED_DEFAULT;
    private int timeout = APIConstants.OPERATION_POLICY_SANDBOX_TIMEOUT_DEFAULT;
    private int executionTimeout = APIConstants.OPERATION_POLICY_SANDBOX_EXECUTION_TIMEOUT_DEFAULT;
    private int maxConcurrentRuns = APIConstants.OPERATION_POLICY_SANDBOX_MAX_CONCURRENT_RUNS_DEFAULT;
    private int maxLogLines = APIConstants.OPERATION_POLICY_SANDBOX_MAX_LOG_LINES_DEFAULT;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getExecutionTimeout() {
        return executionTimeout;
    }

    public void setExecutionTimeout(int executionTimeout) {
        this.executionTimeout = executionTimeout;
    }

    public int getMaxConcurrentRuns() {
        return maxConcurrentRuns;
    }

    public void setMaxConcurrentRuns(int maxConcurrentRuns) {
        this.maxConcurrentRuns = maxConcurrentRuns;
    }

    public int getMaxLogLines() {
        return maxLogLines;
    }

    public void setMaxLogLines(int maxLogLines) {
        this.maxLogLines = maxLogLines;
    }
}

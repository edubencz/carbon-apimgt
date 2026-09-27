package org.wso2.carbon.apimgt.rest.api.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;

import javax.xml.bind.annotation.*;
import org.wso2.carbon.apimgt.rest.api.common.annotations.Scope;
import com.fasterxml.jackson.annotation.JsonCreator;



public class PolicySandboxOutboundCallDTO   {
  
    private String nodeId = null;
    private String method = null;
    private String url = null;
    private Boolean mocked = null;
    private String mockId = null;
    private Integer status = null;
    private String requestBody = null;
    private Long durationMs = null;

  /**
   **/
  public PolicySandboxOutboundCallDTO nodeId(String nodeId) {
    this.nodeId = nodeId;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("nodeId")
  public String getNodeId() {
    return nodeId;
  }
  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  /**
   **/
  public PolicySandboxOutboundCallDTO method(String method) {
    this.method = method;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("method")
  public String getMethod() {
    return method;
  }
  public void setMethod(String method) {
    this.method = method;
  }

  /**
   **/
  public PolicySandboxOutboundCallDTO url(String url) {
    this.url = url;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("url")
  public String getUrl() {
    return url;
  }
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   **/
  public PolicySandboxOutboundCallDTO mocked(Boolean mocked) {
    this.mocked = mocked;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("mocked")
  public Boolean isMocked() {
    return mocked;
  }
  public void setMocked(Boolean mocked) {
    this.mocked = mocked;
  }

  /**
   **/
  public PolicySandboxOutboundCallDTO mockId(String mockId) {
    this.mockId = mockId;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("mockId")
  public String getMockId() {
    return mockId;
  }
  public void setMockId(String mockId) {
    this.mockId = mockId;
  }

  /**
   **/
  public PolicySandboxOutboundCallDTO status(Integer status) {
    this.status = status;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("status")
  public Integer getStatus() {
    return status;
  }
  public void setStatus(Integer status) {
    this.status = status;
  }

  /**
   **/
  public PolicySandboxOutboundCallDTO requestBody(String requestBody) {
    this.requestBody = requestBody;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("requestBody")
  public String getRequestBody() {
    return requestBody;
  }
  public void setRequestBody(String requestBody) {
    this.requestBody = requestBody;
  }

  /**
   **/
  public PolicySandboxOutboundCallDTO durationMs(Long durationMs) {
    this.durationMs = durationMs;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("durationMs")
  public Long getDurationMs() {
    return durationMs;
  }
  public void setDurationMs(Long durationMs) {
    this.durationMs = durationMs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicySandboxOutboundCallDTO policySandboxOutboundCall = (PolicySandboxOutboundCallDTO) o;
    return Objects.equals(nodeId, policySandboxOutboundCall.nodeId) &&
        Objects.equals(method, policySandboxOutboundCall.method) &&
        Objects.equals(url, policySandboxOutboundCall.url) &&
        Objects.equals(mocked, policySandboxOutboundCall.mocked) &&
        Objects.equals(mockId, policySandboxOutboundCall.mockId) &&
        Objects.equals(status, policySandboxOutboundCall.status) &&
        Objects.equals(requestBody, policySandboxOutboundCall.requestBody) &&
        Objects.equals(durationMs, policySandboxOutboundCall.durationMs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeId, method, url, mocked, mockId, status, requestBody, durationMs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PolicySandboxOutboundCallDTO {\n");
    
    sb.append("    nodeId: ").append(toIndentedString(nodeId)).append("\n");
    sb.append("    method: ").append(toIndentedString(method)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    mocked: ").append(toIndentedString(mocked)).append("\n");
    sb.append("    mockId: ").append(toIndentedString(mockId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    requestBody: ").append(toIndentedString(requestBody)).append("\n");
    sb.append("    durationMs: ").append(toIndentedString(durationMs)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}


package org.wso2.carbon.apimgt.rest.api.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;

import javax.xml.bind.annotation.*;
import org.wso2.carbon.apimgt.rest.api.common.annotations.Scope;
import com.fasterxml.jackson.annotation.JsonCreator;



public class PolicySandboxFinalMessageDTO   {
  
    private String payload = null;
    private String contentType = null;
    private Integer httpStatus = null;
    private Map<String, String> headers = new HashMap<>();

  /**
   **/
  public PolicySandboxFinalMessageDTO payload(String payload) {
    this.payload = payload;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("payload")
  public String getPayload() {
    return payload;
  }
  public void setPayload(String payload) {
    this.payload = payload;
  }

  /**
   **/
  public PolicySandboxFinalMessageDTO contentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("contentType")
  public String getContentType() {
    return contentType;
  }
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  /**
   **/
  public PolicySandboxFinalMessageDTO httpStatus(Integer httpStatus) {
    this.httpStatus = httpStatus;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("httpStatus")
  public Integer getHttpStatus() {
    return httpStatus;
  }
  public void setHttpStatus(Integer httpStatus) {
    this.httpStatus = httpStatus;
  }

  /**
   **/
  public PolicySandboxFinalMessageDTO headers(Map<String, String> headers) {
    this.headers = headers;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("headers")
  public Map<String, String> getHeaders() {
    return headers;
  }
  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicySandboxFinalMessageDTO policySandboxFinalMessage = (PolicySandboxFinalMessageDTO) o;
    return Objects.equals(payload, policySandboxFinalMessage.payload) &&
        Objects.equals(contentType, policySandboxFinalMessage.contentType) &&
        Objects.equals(httpStatus, policySandboxFinalMessage.httpStatus) &&
        Objects.equals(headers, policySandboxFinalMessage.headers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(payload, contentType, httpStatus, headers);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PolicySandboxFinalMessageDTO {\n");
    
    sb.append("    payload: ").append(toIndentedString(payload)).append("\n");
    sb.append("    contentType: ").append(toIndentedString(contentType)).append("\n");
    sb.append("    httpStatus: ").append(toIndentedString(httpStatus)).append("\n");
    sb.append("    headers: ").append(toIndentedString(headers)).append("\n");
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


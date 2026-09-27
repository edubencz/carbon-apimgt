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



public class PolicySandboxSampleRequestDTO   {
  
    private String method = null;
    private String path = null;
    private Map<String, String> headers = new HashMap<>();
    private String body = null;
    private String contentType = null;

  /**
   **/
  public PolicySandboxSampleRequestDTO method(String method) {
    this.method = method;
    return this;
  }

  
  @ApiModelProperty(example = "POST", value = "")
  @JsonProperty("method")
  public String getMethod() {
    return method;
  }
  public void setMethod(String method) {
    this.method = method;
  }

  /**
   **/
  public PolicySandboxSampleRequestDTO path(String path) {
    this.path = path;
    return this;
  }

  
  @ApiModelProperty(example = "/orders/1?x=y", value = "")
  @JsonProperty("path")
  public String getPath() {
    return path;
  }
  public void setPath(String path) {
    this.path = path;
  }

  /**
   **/
  public PolicySandboxSampleRequestDTO headers(Map<String, String> headers) {
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

  /**
   **/
  public PolicySandboxSampleRequestDTO body(String body) {
    this.body = body;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("body")
  public String getBody() {
    return body;
  }
  public void setBody(String body) {
    this.body = body;
  }

  /**
   **/
  public PolicySandboxSampleRequestDTO contentType(String contentType) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicySandboxSampleRequestDTO policySandboxSampleRequest = (PolicySandboxSampleRequestDTO) o;
    return Objects.equals(method, policySandboxSampleRequest.method) &&
        Objects.equals(path, policySandboxSampleRequest.path) &&
        Objects.equals(headers, policySandboxSampleRequest.headers) &&
        Objects.equals(body, policySandboxSampleRequest.body) &&
        Objects.equals(contentType, policySandboxSampleRequest.contentType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, path, headers, body, contentType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PolicySandboxSampleRequestDTO {\n");
    
    sb.append("    method: ").append(toIndentedString(method)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    headers: ").append(toIndentedString(headers)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    contentType: ").append(toIndentedString(contentType)).append("\n");
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


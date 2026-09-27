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



public class PolicySandboxClientResponseDTO   {
  
    private Integer status = null;
    private Map<String, String> headers = new HashMap<>();
    private String body = null;

  /**
   **/
  public PolicySandboxClientResponseDTO status(Integer status) {
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
  public PolicySandboxClientResponseDTO headers(Map<String, String> headers) {
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
  public PolicySandboxClientResponseDTO body(String body) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicySandboxClientResponseDTO policySandboxClientResponse = (PolicySandboxClientResponseDTO) o;
    return Objects.equals(status, policySandboxClientResponse.status) &&
        Objects.equals(headers, policySandboxClientResponse.headers) &&
        Objects.equals(body, policySandboxClientResponse.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, headers, body);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PolicySandboxClientResponseDTO {\n");
    
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    headers: ").append(toIndentedString(headers)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
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


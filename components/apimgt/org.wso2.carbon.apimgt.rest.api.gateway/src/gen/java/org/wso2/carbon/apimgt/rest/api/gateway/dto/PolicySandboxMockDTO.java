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



public class PolicySandboxMockDTO   {
  
    private String id = null;
    private String urlPattern = null;

    @XmlType(name="MatchTypeEnum")
    @XmlEnum(String.class)
    public enum MatchTypeEnum {
        GLOB("GLOB"),
        REGEX("REGEX");
        private String value;

        MatchTypeEnum (String v) {
            value = v;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static MatchTypeEnum fromValue(String v) {
            for (MatchTypeEnum b : MatchTypeEnum.values()) {
                if (String.valueOf(b.value).equals(v)) {
                    return b;
                }
            }
return null;
        }
    }
    private MatchTypeEnum matchType = null;
    private String method = null;
    private Integer status = null;
    private Map<String, String> headers = new HashMap<>();
    private String body = null;
    private String contentType = null;
    private Integer delayMs = null;

  /**
   **/
  public PolicySandboxMockDTO id(String id) {
    this.id = id;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("id")
  @NotNull
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  /**
   * A GLOB or REGEX pattern matched against the rendered endpoint/target URL.
   **/
  public PolicySandboxMockDTO urlPattern(String urlPattern) {
    this.urlPattern = urlPattern;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "A GLOB or REGEX pattern matched against the rendered endpoint/target URL.")
  @JsonProperty("urlPattern")
  @NotNull
  public String getUrlPattern() {
    return urlPattern;
  }
  public void setUrlPattern(String urlPattern) {
    this.urlPattern = urlPattern;
  }

  /**
   **/
  public PolicySandboxMockDTO matchType(MatchTypeEnum matchType) {
    this.matchType = matchType;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("matchType")
  @NotNull
  public MatchTypeEnum getMatchType() {
    return matchType;
  }
  public void setMatchType(MatchTypeEnum matchType) {
    this.matchType = matchType;
  }

  /**
   * Optional; any method matches when absent.
   **/
  public PolicySandboxMockDTO method(String method) {
    this.method = method;
    return this;
  }

  
  @ApiModelProperty(value = "Optional; any method matches when absent.")
  @JsonProperty("method")
  public String getMethod() {
    return method;
  }
  public void setMethod(String method) {
    this.method = method;
  }

  /**
   **/
  public PolicySandboxMockDTO status(Integer status) {
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
  public PolicySandboxMockDTO headers(Map<String, String> headers) {
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
  public PolicySandboxMockDTO body(String body) {
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
  public PolicySandboxMockDTO contentType(String contentType) {
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
  public PolicySandboxMockDTO delayMs(Integer delayMs) {
    this.delayMs = delayMs;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("delayMs")
  public Integer getDelayMs() {
    return delayMs;
  }
  public void setDelayMs(Integer delayMs) {
    this.delayMs = delayMs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicySandboxMockDTO policySandboxMock = (PolicySandboxMockDTO) o;
    return Objects.equals(id, policySandboxMock.id) &&
        Objects.equals(urlPattern, policySandboxMock.urlPattern) &&
        Objects.equals(matchType, policySandboxMock.matchType) &&
        Objects.equals(method, policySandboxMock.method) &&
        Objects.equals(status, policySandboxMock.status) &&
        Objects.equals(headers, policySandboxMock.headers) &&
        Objects.equals(body, policySandboxMock.body) &&
        Objects.equals(contentType, policySandboxMock.contentType) &&
        Objects.equals(delayMs, policySandboxMock.delayMs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, urlPattern, matchType, method, status, headers, body, contentType, delayMs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PolicySandboxMockDTO {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    urlPattern: ").append(toIndentedString(urlPattern)).append("\n");
    sb.append("    matchType: ").append(toIndentedString(matchType)).append("\n");
    sb.append("    method: ").append(toIndentedString(method)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    headers: ").append(toIndentedString(headers)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    contentType: ").append(toIndentedString(contentType)).append("\n");
    sb.append("    delayMs: ").append(toIndentedString(delayMs)).append("\n");
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


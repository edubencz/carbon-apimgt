package org.wso2.carbon.apimgt.rest.api.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;

import javax.xml.bind.annotation.*;
import org.wso2.carbon.apimgt.rest.api.common.annotations.Scope;
import com.fasterxml.jackson.annotation.JsonCreator;



public class PolicySandboxLogEntryDTO   {
  
    private Long ts = null;
    private String level = null;
    private String nodeId = null;
    private String message = null;

  /**
   **/
  public PolicySandboxLogEntryDTO ts(Long ts) {
    this.ts = ts;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("ts")
  public Long getTs() {
    return ts;
  }
  public void setTs(Long ts) {
    this.ts = ts;
  }

  /**
   **/
  public PolicySandboxLogEntryDTO level(String level) {
    this.level = level;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("level")
  public String getLevel() {
    return level;
  }
  public void setLevel(String level) {
    this.level = level;
  }

  /**
   **/
  public PolicySandboxLogEntryDTO nodeId(String nodeId) {
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
  public PolicySandboxLogEntryDTO message(String message) {
    this.message = message;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }
  public void setMessage(String message) {
    this.message = message;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicySandboxLogEntryDTO policySandboxLogEntry = (PolicySandboxLogEntryDTO) o;
    return Objects.equals(ts, policySandboxLogEntry.ts) &&
        Objects.equals(level, policySandboxLogEntry.level) &&
        Objects.equals(nodeId, policySandboxLogEntry.nodeId) &&
        Objects.equals(message, policySandboxLogEntry.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ts, level, nodeId, message);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PolicySandboxLogEntryDTO {\n");
    
    sb.append("    ts: ").append(toIndentedString(ts)).append("\n");
    sb.append("    level: ").append(toIndentedString(level)).append("\n");
    sb.append("    nodeId: ").append(toIndentedString(nodeId)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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


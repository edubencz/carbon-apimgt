package org.wso2.carbon.apimgt.rest.api.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;

import javax.xml.bind.annotation.*;
import org.wso2.carbon.apimgt.rest.api.common.annotations.Scope;
import com.fasterxml.jackson.annotation.JsonCreator;



public class PolicySandboxTraceEntryDTO   {
  
    private Integer order = null;
    private String nodeId = null;
    private String tag = null;
    private Long tMs = null;

  /**
   **/
  public PolicySandboxTraceEntryDTO order(Integer order) {
    this.order = order;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("order")
  public Integer getOrder() {
    return order;
  }
  public void setOrder(Integer order) {
    this.order = order;
  }

  /**
   **/
  public PolicySandboxTraceEntryDTO nodeId(String nodeId) {
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
  public PolicySandboxTraceEntryDTO tag(String tag) {
    this.tag = tag;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("tag")
  public String getTag() {
    return tag;
  }
  public void setTag(String tag) {
    this.tag = tag;
  }

  /**
   **/
  public PolicySandboxTraceEntryDTO tMs(Long tMs) {
    this.tMs = tMs;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("tMs")
  public Long getTMs() {
    return tMs;
  }
  public void setTMs(Long tMs) {
    this.tMs = tMs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicySandboxTraceEntryDTO policySandboxTraceEntry = (PolicySandboxTraceEntryDTO) o;
    return Objects.equals(order, policySandboxTraceEntry.order) &&
        Objects.equals(nodeId, policySandboxTraceEntry.nodeId) &&
        Objects.equals(tag, policySandboxTraceEntry.tag) &&
        Objects.equals(tMs, policySandboxTraceEntry.tMs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(order, nodeId, tag, tMs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PolicySandboxTraceEntryDTO {\n");
    
    sb.append("    order: ").append(toIndentedString(order)).append("\n");
    sb.append("    nodeId: ").append(toIndentedString(nodeId)).append("\n");
    sb.append("    tag: ").append(toIndentedString(tag)).append("\n");
    sb.append("    tMs: ").append(toIndentedString(tMs)).append("\n");
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


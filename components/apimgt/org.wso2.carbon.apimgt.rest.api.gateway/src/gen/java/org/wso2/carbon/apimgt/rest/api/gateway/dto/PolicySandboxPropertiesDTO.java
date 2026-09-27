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



public class PolicySandboxPropertiesDTO   {
  
    private Map<String, String> synapse = new HashMap<>();
    private Map<String, String> axis2 = new HashMap<>();
    private Map<String, String> transport = new HashMap<>();

  /**
   **/
  public PolicySandboxPropertiesDTO synapse(Map<String, String> synapse) {
    this.synapse = synapse;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("synapse")
  public Map<String, String> getSynapse() {
    return synapse;
  }
  public void setSynapse(Map<String, String> synapse) {
    this.synapse = synapse;
  }

  /**
   **/
  public PolicySandboxPropertiesDTO axis2(Map<String, String> axis2) {
    this.axis2 = axis2;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("axis2")
  public Map<String, String> getAxis2() {
    return axis2;
  }
  public void setAxis2(Map<String, String> axis2) {
    this.axis2 = axis2;
  }

  /**
   **/
  public PolicySandboxPropertiesDTO transport(Map<String, String> transport) {
    this.transport = transport;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("transport")
  public Map<String, String> getTransport() {
    return transport;
  }
  public void setTransport(Map<String, String> transport) {
    this.transport = transport;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicySandboxPropertiesDTO policySandboxProperties = (PolicySandboxPropertiesDTO) o;
    return Objects.equals(synapse, policySandboxProperties.synapse) &&
        Objects.equals(axis2, policySandboxProperties.axis2) &&
        Objects.equals(transport, policySandboxProperties.transport);
  }

  @Override
  public int hashCode() {
    return Objects.hash(synapse, axis2, transport);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PolicySandboxPropertiesDTO {\n");
    
    sb.append("    synapse: ").append(toIndentedString(synapse)).append("\n");
    sb.append("    axis2: ").append(toIndentedString(axis2)).append("\n");
    sb.append("    transport: ").append(toIndentedString(transport)).append("\n");
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


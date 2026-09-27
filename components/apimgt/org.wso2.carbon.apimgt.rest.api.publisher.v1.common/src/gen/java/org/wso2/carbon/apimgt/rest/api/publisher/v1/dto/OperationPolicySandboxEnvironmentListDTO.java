package org.wso2.carbon.apimgt.rest.api.publisher.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.dto.OperationPolicySandboxEnvironmentDTO;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;

import javax.xml.bind.annotation.*;
import org.wso2.carbon.apimgt.rest.api.common.annotations.Scope;
import com.fasterxml.jackson.annotation.JsonCreator;

import javax.validation.Valid;



public class OperationPolicySandboxEnvironmentListDTO   {
  
    private Boolean enabled = null;
    private List<OperationPolicySandboxEnvironmentDTO> environments = new ArrayList<OperationPolicySandboxEnvironmentDTO>();

  /**
   **/
  public OperationPolicySandboxEnvironmentListDTO enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("enabled")
  public Boolean isEnabled() {
    return enabled;
  }
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   **/
  public OperationPolicySandboxEnvironmentListDTO environments(List<OperationPolicySandboxEnvironmentDTO> environments) {
    this.environments = environments;
    return this;
  }

  
  @ApiModelProperty(value = "")
      @Valid
  @JsonProperty("environments")
  public List<OperationPolicySandboxEnvironmentDTO> getEnvironments() {
    return environments;
  }
  public void setEnvironments(List<OperationPolicySandboxEnvironmentDTO> environments) {
    this.environments = environments;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OperationPolicySandboxEnvironmentListDTO operationPolicySandboxEnvironmentList = (OperationPolicySandboxEnvironmentListDTO) o;
    return Objects.equals(enabled, operationPolicySandboxEnvironmentList.enabled) &&
        Objects.equals(environments, operationPolicySandboxEnvironmentList.environments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, environments);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OperationPolicySandboxEnvironmentListDTO {\n");
    
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    environments: ").append(toIndentedString(environments)).append("\n");
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


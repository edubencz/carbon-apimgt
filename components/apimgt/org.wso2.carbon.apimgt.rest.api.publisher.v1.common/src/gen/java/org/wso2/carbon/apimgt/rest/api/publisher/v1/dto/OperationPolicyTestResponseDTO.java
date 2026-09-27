package org.wso2.carbon.apimgt.rest.api.publisher.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.dto.OperationPolicyRenderErrorDTO;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;

import javax.xml.bind.annotation.*;
import org.wso2.carbon.apimgt.rest.api.common.annotations.Scope;
import com.fasterxml.jackson.annotation.JsonCreator;

import javax.validation.Valid;



public class OperationPolicyTestResponseDTO   {
  
    private String renderedSequence = null;
    private Boolean normalized = null;
    private List<OperationPolicyRenderErrorDTO> errors = new ArrayList<OperationPolicyRenderErrorDTO>();
    private List<String> warnings = new ArrayList<String>();
    private List<String> detectedVariables = new ArrayList<String>();
    private List<String> unknownMediators = new ArrayList<String>();
    private Map<String, Object> execution = new HashMap<String, Object>();

  /**
   **/
  public OperationPolicyTestResponseDTO renderedSequence(String renderedSequence) {
    this.renderedSequence = renderedSequence;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("renderedSequence")
  public String getRenderedSequence() {
    return renderedSequence;
  }
  public void setRenderedSequence(String renderedSequence) {
    this.renderedSequence = renderedSequence;
  }

  /**
   **/
  public OperationPolicyTestResponseDTO normalized(Boolean normalized) {
    this.normalized = normalized;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("normalized")
  public Boolean isNormalized() {
    return normalized;
  }
  public void setNormalized(Boolean normalized) {
    this.normalized = normalized;
  }

  /**
   **/
  public OperationPolicyTestResponseDTO errors(List<OperationPolicyRenderErrorDTO> errors) {
    this.errors = errors;
    return this;
  }

  
  @ApiModelProperty(value = "")
      @Valid
  @JsonProperty("errors")
  public List<OperationPolicyRenderErrorDTO> getErrors() {
    return errors;
  }
  public void setErrors(List<OperationPolicyRenderErrorDTO> errors) {
    this.errors = errors;
  }

  /**
   **/
  public OperationPolicyTestResponseDTO warnings(List<String> warnings) {
    this.warnings = warnings;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("warnings")
  public List<String> getWarnings() {
    return warnings;
  }
  public void setWarnings(List<String> warnings) {
    this.warnings = warnings;
  }

  /**
   **/
  public OperationPolicyTestResponseDTO detectedVariables(List<String> detectedVariables) {
    this.detectedVariables = detectedVariables;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("detectedVariables")
  public List<String> getDetectedVariables() {
    return detectedVariables;
  }
  public void setDetectedVariables(List<String> detectedVariables) {
    this.detectedVariables = detectedVariables;
  }

  /**
   **/
  public OperationPolicyTestResponseDTO unknownMediators(List<String> unknownMediators) {
    this.unknownMediators = unknownMediators;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("unknownMediators")
  public List<String> getUnknownMediators() {
    return unknownMediators;
  }
  public void setUnknownMediators(List<String> unknownMediators) {
    this.unknownMediators = unknownMediators;
  }

  /**
   * The gateway&#39;s PolicySandboxExecuteResponse, passed through verbatim.
   **/
  public OperationPolicyTestResponseDTO execution(Map<String, Object> execution) {
    this.execution = execution;
    return this;
  }

  
  @ApiModelProperty(value = "The gateway's PolicySandboxExecuteResponse, passed through verbatim.")
  @JsonProperty("execution")
  public Map<String, Object> getExecution() {
    return execution;
  }
  public void setExecution(Map<String, Object> execution) {
    this.execution = execution;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OperationPolicyTestResponseDTO operationPolicyTestResponse = (OperationPolicyTestResponseDTO) o;
    return Objects.equals(renderedSequence, operationPolicyTestResponse.renderedSequence) &&
        Objects.equals(normalized, operationPolicyTestResponse.normalized) &&
        Objects.equals(errors, operationPolicyTestResponse.errors) &&
        Objects.equals(warnings, operationPolicyTestResponse.warnings) &&
        Objects.equals(detectedVariables, operationPolicyTestResponse.detectedVariables) &&
        Objects.equals(unknownMediators, operationPolicyTestResponse.unknownMediators) &&
        Objects.equals(execution, operationPolicyTestResponse.execution);
  }

  @Override
  public int hashCode() {
    return Objects.hash(renderedSequence, normalized, errors, warnings, detectedVariables, unknownMediators, execution);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OperationPolicyTestResponseDTO {\n");
    
    sb.append("    renderedSequence: ").append(toIndentedString(renderedSequence)).append("\n");
    sb.append("    normalized: ").append(toIndentedString(normalized)).append("\n");
    sb.append("    errors: ").append(toIndentedString(errors)).append("\n");
    sb.append("    warnings: ").append(toIndentedString(warnings)).append("\n");
    sb.append("    detectedVariables: ").append(toIndentedString(detectedVariables)).append("\n");
    sb.append("    unknownMediators: ").append(toIndentedString(unknownMediators)).append("\n");
    sb.append("    execution: ").append(toIndentedString(execution)).append("\n");
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


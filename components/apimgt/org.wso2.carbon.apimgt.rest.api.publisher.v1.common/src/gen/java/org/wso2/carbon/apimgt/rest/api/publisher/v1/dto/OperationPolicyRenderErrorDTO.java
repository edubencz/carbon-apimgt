package org.wso2.carbon.apimgt.rest.api.publisher.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;

import javax.xml.bind.annotation.*;
import org.wso2.carbon.apimgt.rest.api.common.annotations.Scope;
import com.fasterxml.jackson.annotation.JsonCreator;

import javax.validation.Valid;



public class OperationPolicyRenderErrorDTO   {
  
    private Integer line = null;
    private Integer column = null;
    private String message = null;
    private String severity = null;

  /**
   **/
  public OperationPolicyRenderErrorDTO line(Integer line) {
    this.line = line;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("line")
  public Integer getLine() {
    return line;
  }
  public void setLine(Integer line) {
    this.line = line;
  }

  /**
   **/
  public OperationPolicyRenderErrorDTO column(Integer column) {
    this.column = column;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("column")
  public Integer getColumn() {
    return column;
  }
  public void setColumn(Integer column) {
    this.column = column;
  }

  /**
   **/
  public OperationPolicyRenderErrorDTO message(String message) {
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

  /**
   **/
  public OperationPolicyRenderErrorDTO severity(String severity) {
    this.severity = severity;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("severity")
  public String getSeverity() {
    return severity;
  }
  public void setSeverity(String severity) {
    this.severity = severity;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OperationPolicyRenderErrorDTO operationPolicyRenderError = (OperationPolicyRenderErrorDTO) o;
    return Objects.equals(line, operationPolicyRenderError.line) &&
        Objects.equals(column, operationPolicyRenderError.column) &&
        Objects.equals(message, operationPolicyRenderError.message) &&
        Objects.equals(severity, operationPolicyRenderError.severity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(line, column, message, severity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OperationPolicyRenderErrorDTO {\n");
    
    sb.append("    line: ").append(toIndentedString(line)).append("\n");
    sb.append("    column: ").append(toIndentedString(column)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    severity: ").append(toIndentedString(severity)).append("\n");
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


package org.wso2.carbon.apimgt.rest.api.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.List;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxClientResponseDTO;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxFaultDTO;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxFinalMessageDTO;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxLogEntryDTO;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxOutboundCallDTO;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxPropertiesDTO;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxTraceEntryDTO;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;

import javax.xml.bind.annotation.*;
import org.wso2.carbon.apimgt.rest.api.common.annotations.Scope;
import com.fasterxml.jackson.annotation.JsonCreator;



public class PolicySandboxExecuteResponseDTO   {
  

    @XmlType(name="StatusEnum")
    @XmlEnum(String.class)
    public enum StatusEnum {
        COMPLETED("COMPLETED"),
        RESPONDED("RESPONDED"),
        FAULT("FAULT"),
        DROPPED("DROPPED"),
        TIMEOUT("TIMEOUT"),
        ERROR("ERROR");
        private String value;

        StatusEnum (String v) {
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
        public static StatusEnum fromValue(String v) {
            for (StatusEnum b : StatusEnum.values()) {
                if (String.valueOf(b.value).equals(v)) {
                    return b;
                }
            }
return null;
        }
    }
    private StatusEnum status = null;
    private Boolean respondedEarly = null;
    private Long durationMs = null;
    private PolicySandboxClientResponseDTO clientResponse = null;
    private PolicySandboxFinalMessageDTO finalMessage = null;
    private PolicySandboxPropertiesDTO properties = null;
    private List<PolicySandboxLogEntryDTO> logs = new ArrayList<>();
    private List<PolicySandboxTraceEntryDTO> trace = new ArrayList<>();
    private List<PolicySandboxOutboundCallDTO> outboundCalls = new ArrayList<>();
    private PolicySandboxFaultDTO fault = null;
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

  /**
   **/
  public PolicySandboxExecuteResponseDTO status(StatusEnum status) {
    this.status = status;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }
  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  /**
   **/
  public PolicySandboxExecuteResponseDTO respondedEarly(Boolean respondedEarly) {
    this.respondedEarly = respondedEarly;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("respondedEarly")
  public Boolean isRespondedEarly() {
    return respondedEarly;
  }
  public void setRespondedEarly(Boolean respondedEarly) {
    this.respondedEarly = respondedEarly;
  }

  /**
   **/
  public PolicySandboxExecuteResponseDTO durationMs(Long durationMs) {
    this.durationMs = durationMs;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("durationMs")
  public Long getDurationMs() {
    return durationMs;
  }
  public void setDurationMs(Long durationMs) {
    this.durationMs = durationMs;
  }

  /**
   **/
  public PolicySandboxExecuteResponseDTO clientResponse(PolicySandboxClientResponseDTO clientResponse) {
    this.clientResponse = clientResponse;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("clientResponse")
  public PolicySandboxClientResponseDTO getClientResponse() {
    return clientResponse;
  }
  public void setClientResponse(PolicySandboxClientResponseDTO clientResponse) {
    this.clientResponse = clientResponse;
  }

  /**
   **/
  public PolicySandboxExecuteResponseDTO finalMessage(PolicySandboxFinalMessageDTO finalMessage) {
    this.finalMessage = finalMessage;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("finalMessage")
  public PolicySandboxFinalMessageDTO getFinalMessage() {
    return finalMessage;
  }
  public void setFinalMessage(PolicySandboxFinalMessageDTO finalMessage) {
    this.finalMessage = finalMessage;
  }

  /**
   **/
  public PolicySandboxExecuteResponseDTO properties(PolicySandboxPropertiesDTO properties) {
    this.properties = properties;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("properties")
  public PolicySandboxPropertiesDTO getProperties() {
    return properties;
  }
  public void setProperties(PolicySandboxPropertiesDTO properties) {
    this.properties = properties;
  }

  /**
   **/
  public PolicySandboxExecuteResponseDTO logs(List<PolicySandboxLogEntryDTO> logs) {
    this.logs = logs;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("logs")
  public List<PolicySandboxLogEntryDTO> getLogs() {
    return logs;
  }
  public void setLogs(List<PolicySandboxLogEntryDTO> logs) {
    this.logs = logs;
  }

  /**
   **/
  public PolicySandboxExecuteResponseDTO trace(List<PolicySandboxTraceEntryDTO> trace) {
    this.trace = trace;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("trace")
  public List<PolicySandboxTraceEntryDTO> getTrace() {
    return trace;
  }
  public void setTrace(List<PolicySandboxTraceEntryDTO> trace) {
    this.trace = trace;
  }

  /**
   **/
  public PolicySandboxExecuteResponseDTO outboundCalls(List<PolicySandboxOutboundCallDTO> outboundCalls) {
    this.outboundCalls = outboundCalls;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("outboundCalls")
  public List<PolicySandboxOutboundCallDTO> getOutboundCalls() {
    return outboundCalls;
  }
  public void setOutboundCalls(List<PolicySandboxOutboundCallDTO> outboundCalls) {
    this.outboundCalls = outboundCalls;
  }

  /**
   **/
  public PolicySandboxExecuteResponseDTO fault(PolicySandboxFaultDTO fault) {
    this.fault = fault;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("fault")
  public PolicySandboxFaultDTO getFault() {
    return fault;
  }
  public void setFault(PolicySandboxFaultDTO fault) {
    this.fault = fault;
  }

  /**
   **/
  public PolicySandboxExecuteResponseDTO warnings(List<String> warnings) {
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
  public PolicySandboxExecuteResponseDTO errors(List<String> errors) {
    this.errors = errors;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("errors")
  public List<String> getErrors() {
    return errors;
  }
  public void setErrors(List<String> errors) {
    this.errors = errors;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicySandboxExecuteResponseDTO policySandboxExecuteResponse = (PolicySandboxExecuteResponseDTO) o;
    return Objects.equals(status, policySandboxExecuteResponse.status) &&
        Objects.equals(respondedEarly, policySandboxExecuteResponse.respondedEarly) &&
        Objects.equals(durationMs, policySandboxExecuteResponse.durationMs) &&
        Objects.equals(clientResponse, policySandboxExecuteResponse.clientResponse) &&
        Objects.equals(finalMessage, policySandboxExecuteResponse.finalMessage) &&
        Objects.equals(properties, policySandboxExecuteResponse.properties) &&
        Objects.equals(logs, policySandboxExecuteResponse.logs) &&
        Objects.equals(trace, policySandboxExecuteResponse.trace) &&
        Objects.equals(outboundCalls, policySandboxExecuteResponse.outboundCalls) &&
        Objects.equals(fault, policySandboxExecuteResponse.fault) &&
        Objects.equals(warnings, policySandboxExecuteResponse.warnings) &&
        Objects.equals(errors, policySandboxExecuteResponse.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, respondedEarly, durationMs, clientResponse, finalMessage, properties, logs, trace, outboundCalls, fault, warnings, errors);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PolicySandboxExecuteResponseDTO {\n");
    
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    respondedEarly: ").append(toIndentedString(respondedEarly)).append("\n");
    sb.append("    durationMs: ").append(toIndentedString(durationMs)).append("\n");
    sb.append("    clientResponse: ").append(toIndentedString(clientResponse)).append("\n");
    sb.append("    finalMessage: ").append(toIndentedString(finalMessage)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    logs: ").append(toIndentedString(logs)).append("\n");
    sb.append("    trace: ").append(toIndentedString(trace)).append("\n");
    sb.append("    outboundCalls: ").append(toIndentedString(outboundCalls)).append("\n");
    sb.append("    fault: ").append(toIndentedString(fault)).append("\n");
    sb.append("    warnings: ").append(toIndentedString(warnings)).append("\n");
    sb.append("    errors: ").append(toIndentedString(errors)).append("\n");
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


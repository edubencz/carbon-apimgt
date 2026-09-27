package org.wso2.carbon.apimgt.rest.api.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxMockDTO;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxSampleRequestDTO;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;

import javax.xml.bind.annotation.*;
import org.wso2.carbon.apimgt.rest.api.common.annotations.Scope;
import com.fasterxml.jackson.annotation.JsonCreator;



public class PolicySandboxExecuteRequestDTO   {
  
    private String renderedSequence = null;

    @XmlType(name="FlowEnum")
    @XmlEnum(String.class)
    public enum FlowEnum {
        REQUEST("request"),
        RESPONSE("response"),
        FAULT("fault");
        private String value;

        FlowEnum (String v) {
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
        public static FlowEnum fromValue(String v) {
            for (FlowEnum b : FlowEnum.values()) {
                if (String.valueOf(b.value).equals(v)) {
                    return b;
                }
            }
return null;
        }
    }
    private FlowEnum flow = FlowEnum.REQUEST;
    private PolicySandboxSampleRequestDTO sampleRequest = null;
    private List<PolicySandboxMockDTO> mocks = new ArrayList<>();
    private Map<String, String> extraProperties = new HashMap<>();
    private Boolean captureSnapshots = false;

  /**
   * Already-rendered (no Jinja), normalized Synapse mediation sequence: a single &lt;sequence&gt; root, no name attribute. 
   **/
  public PolicySandboxExecuteRequestDTO renderedSequence(String renderedSequence) {
    this.renderedSequence = renderedSequence;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "Already-rendered (no Jinja), normalized Synapse mediation sequence: a single <sequence> root, no name attribute. ")
  @JsonProperty("renderedSequence")
  @NotNull
  public String getRenderedSequence() {
    return renderedSequence;
  }
  public void setRenderedSequence(String renderedSequence) {
    this.renderedSequence = renderedSequence;
  }

  /**
   **/
  public PolicySandboxExecuteRequestDTO flow(FlowEnum flow) {
    this.flow = flow;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("flow")
  public FlowEnum getFlow() {
    return flow;
  }
  public void setFlow(FlowEnum flow) {
    this.flow = flow;
  }

  /**
   **/
  public PolicySandboxExecuteRequestDTO sampleRequest(PolicySandboxSampleRequestDTO sampleRequest) {
    this.sampleRequest = sampleRequest;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("sampleRequest")
  public PolicySandboxSampleRequestDTO getSampleRequest() {
    return sampleRequest;
  }
  public void setSampleRequest(PolicySandboxSampleRequestDTO sampleRequest) {
    this.sampleRequest = sampleRequest;
  }

  /**
   **/
  public PolicySandboxExecuteRequestDTO mocks(List<PolicySandboxMockDTO> mocks) {
    this.mocks = mocks;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("mocks")
  public List<PolicySandboxMockDTO> getMocks() {
    return mocks;
  }
  public void setMocks(List<PolicySandboxMockDTO> mocks) {
    this.mocks = mocks;
  }

  /**
   **/
  public PolicySandboxExecuteRequestDTO extraProperties(Map<String, String> extraProperties) {
    this.extraProperties = extraProperties;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("extraProperties")
  public Map<String, String> getExtraProperties() {
    return extraProperties;
  }
  public void setExtraProperties(Map<String, String> extraProperties) {
    this.extraProperties = extraProperties;
  }

  /**
   **/
  public PolicySandboxExecuteRequestDTO captureSnapshots(Boolean captureSnapshots) {
    this.captureSnapshots = captureSnapshots;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("captureSnapshots")
  public Boolean isCaptureSnapshots() {
    return captureSnapshots;
  }
  public void setCaptureSnapshots(Boolean captureSnapshots) {
    this.captureSnapshots = captureSnapshots;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicySandboxExecuteRequestDTO policySandboxExecuteRequest = (PolicySandboxExecuteRequestDTO) o;
    return Objects.equals(renderedSequence, policySandboxExecuteRequest.renderedSequence) &&
        Objects.equals(flow, policySandboxExecuteRequest.flow) &&
        Objects.equals(sampleRequest, policySandboxExecuteRequest.sampleRequest) &&
        Objects.equals(mocks, policySandboxExecuteRequest.mocks) &&
        Objects.equals(extraProperties, policySandboxExecuteRequest.extraProperties) &&
        Objects.equals(captureSnapshots, policySandboxExecuteRequest.captureSnapshots);
  }

  @Override
  public int hashCode() {
    return Objects.hash(renderedSequence, flow, sampleRequest, mocks, extraProperties, captureSnapshots);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PolicySandboxExecuteRequestDTO {\n");
    
    sb.append("    renderedSequence: ").append(toIndentedString(renderedSequence)).append("\n");
    sb.append("    flow: ").append(toIndentedString(flow)).append("\n");
    sb.append("    sampleRequest: ").append(toIndentedString(sampleRequest)).append("\n");
    sb.append("    mocks: ").append(toIndentedString(mocks)).append("\n");
    sb.append("    extraProperties: ").append(toIndentedString(extraProperties)).append("\n");
    sb.append("    captureSnapshots: ").append(toIndentedString(captureSnapshots)).append("\n");
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


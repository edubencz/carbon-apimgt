package org.wso2.carbon.apimgt.rest.api.gateway;

import org.wso2.carbon.apimgt.rest.api.gateway.dto.ErrorDTO;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxExecuteRequestDTO;
import org.wso2.carbon.apimgt.rest.api.gateway.dto.PolicySandboxExecuteResponseDTO;
import org.wso2.carbon.apimgt.rest.api.gateway.PolicySandboxApiService;
import org.wso2.carbon.apimgt.rest.api.gateway.impl.PolicySandboxApiServiceImpl;
import org.wso2.carbon.apimgt.api.APIManagementException;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.inject.Inject;

import io.swagger.annotations.*;
import java.io.InputStream;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import java.util.Map;
import java.util.List;
import javax.validation.constraints.*;
@Path("/policy-sandbox")

@Api(description = "the policy-sandbox API")
@Consumes({ "application/json" })
@Produces({ "application/json" })


public class PolicySandboxApi  {

  @Context MessageContext securityContext;

PolicySandboxApiService delegate = new PolicySandboxApiServiceImpl();


    @POST
    @Path("/execute")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Execute a rendered operation-policy sequence in the sandbox", notes = "Deploys the given, already-rendered Synapse mediation sequence as a temporary API, invokes it over loopback with the given sample request (optionally rewriting outbound calls to per-run mocks), captures trace/logs/final-message state, and undeploys the temporary API - always, even on error. Disabled (404) unless [apim.policy_sandbox] enable=true. ", response = PolicySandboxExecuteResponseDTO.class, tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "OK. The sandbox run was attempted (see the response `status` for its outcome). ", response = PolicySandboxExecuteResponseDTO.class),
        @ApiResponse(code = 400, message = "Bad Request. The renderedSequence is not well-formed XML. ", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Not Found. The policy sandbox is disabled on this Gateway. ", response = ErrorDTO.class),
        @ApiResponse(code = 413, message = "Payload Too Large. The sample request body exceeds the configured limit. ", response = ErrorDTO.class),
        @ApiResponse(code = 429, message = "Too Many Requests. The configured max_concurrent_runs limit was exceeded. ", response = ErrorDTO.class) })
    public Response executePolicySandbox(@ApiParam(value = "" ,required=true) PolicySandboxExecuteRequestDTO body) throws APIManagementException{
        return delegate.executePolicySandbox(body, securityContext);
    }
}

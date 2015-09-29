package com.devicehive.resource;

import com.devicehive.model.AccessKeyRequest;
import com.wordnik.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(tags = {"Authentication"}, description = "Auth access key operations", consumes="application/json")
@Path("/auth/accesskey")
public interface AuthAccessKeyResource {

    @POST
    @PreAuthorize("permitAll")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Login")
    @ApiResponses({
            @ApiResponse(code = 403, message = "If identity provider is not allowed")
    })
    Response login(
            @ApiParam(value = "Access key request", required = true)
            AccessKeyRequest request);

    @DELETE
    @PreAuthorize("hasRole('KEY') and hasPermission(null, 'MANAGE_ACCESS_KEY')")
    @ApiOperation(value = "Logout")
    Response logout();

}

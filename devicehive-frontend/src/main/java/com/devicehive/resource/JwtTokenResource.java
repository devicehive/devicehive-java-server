package com.devicehive.resource;

import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

/**
 * REST controller for JwtToken.
 */
@Path("/token")
@Api(tags = {"JwtToken"}, description = "Represents an JWT access/refresh tokens management to API/device.",
        consumes = "application/x-www-form-urlencoded")
@Produces({"application/json"})
public interface JwtTokenResource {

    String AUTH_HEADER = "auth_header";
    String REFRESH_TOKEN = "refresh_token";
    String PASSWORD = "password";
    String CLIENT_CREDENTIALS = "client_credentials";

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @PreAuthorize("permitAll")
    @ApiOperation(value = "JWT token request")
    @ApiResponses({
        @ApiResponse(code = 404, message = "If access token not found")
    })
    Response tokenRequest(
            @ApiParam(name = "access_key", value = "Access key", required = true)
            @FormParam("access_key")
            @NotNull
            String accessKey,
            @ApiParam(name = "grant_type", value = "Grant type", required = true)
            @FormParam("grant_type")
            @NotNull
            String grantType,
            @ApiParam(name = "client_credentials", value = "Client credentials", required = true)
            @FormParam("client_credentials")
            String clientCredentials);
}



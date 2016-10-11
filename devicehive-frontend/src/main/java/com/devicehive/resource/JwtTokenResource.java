package com.devicehive.resource;

import com.devicehive.vo.JwtTokenVO;
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
        consumes = "application/json")
@Produces({"application/json"})
public interface JwtTokenResource {

    String ACCESS_KEY = "access_key";
    String REFRESH_TOKEN = "refresh_token";
    String PASSWORD = "password";

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @PreAuthorize("permitAll")
    @ApiOperation(value = "JWT token request")
    @ApiResponses({
            @ApiResponse(code = 200,
                    message = "If successful, this method returns a JWT token in the response body.",
                    response = JwtTokenVO.class),
        @ApiResponse(code = 404, message = "If access token not found")
    })
    Response tokenRequest(
            @ApiParam(name = "grant_type", value = "Grant type", required = true)
            @QueryParam("grant_type")
            @NotNull
            String grantType,
            @ApiParam(name = "access_key", value = "Access key", required = false)
            @QueryParam("access_key")
            String accessKey,
            @ApiParam(name = "username", value = "User login", required = false)
            @QueryParam("username")
            String username,
            @ApiParam(name = "password", value = "User password", required = false)
            @QueryParam("password")
            String password);
}



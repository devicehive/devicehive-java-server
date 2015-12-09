package com.devicehive.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

@Api(tags = {"Authentication"})
@Path("/oauth2/token")
public interface OAuthTokenResource {
    String AUTHORIZATION_CODE = "authorization_code";
    String PASSWORD = "password";

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @PreAuthorize("permitAll")
    @ApiOperation(value = "Access token request")
    Response accessTokenRequest(
            @ApiParam(name = "grant_type", value = "Grant type", required = true)
            @FormParam("grant_type")
            @NotNull
            String grantType,
            @ApiParam(name = "code", value = "Code", required = true)
            @FormParam("code")
            String code,
            @ApiParam(name = "redirectUri", value = "Redirect Uri", required = true)
            @FormParam("redirectUri")
            String redirectUri,
            @ApiParam(name = "client_id", value = "Client Id", required = true)
            @FormParam("client_id")
            String clientId,
            @ApiParam(name = "scope", value = "Scope", required = true)
            @FormParam("scope")
            String scope,
            @ApiParam(name = "username", value = "User name (Login)", required = true)
            @FormParam("username")
            String login,
            @ApiParam(name = "password", value = "Password", required = true)
            @FormParam("password")
            String password);
}

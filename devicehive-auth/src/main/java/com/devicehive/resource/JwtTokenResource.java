package com.devicehive.resource;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.security.jwt.JwtUserPayloadView;
import com.devicehive.security.jwt.JwtPluginPayload;
import com.devicehive.vo.JwtAccessTokenVO;
import com.devicehive.vo.JwtRefreshTokenVO;
import com.devicehive.vo.JwtRequestVO;
import com.devicehive.vo.JwtTokenVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * REST controller for JwtToken.
 */
@Path("/token")
@Api(tags = {"JwtToken"}, description = "Represents an JWT access/refresh tokens management to API/device.",
        consumes = "application/json")
@Produces({"application/json"})
public interface JwtTokenResource {

    @POST
    @Path("/create")
    @Consumes(APPLICATION_JSON)
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_TOKEN')")
    @ApiOperation(value = "JWT access and refresh token request")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 201,
                    message = "If successful, this method returns a JWT access and refresh token in the response body.",
                    response = JwtTokenVO.class),
            @ApiResponse(code = 404, message = "If access token not found")
    })
    Response tokenRequest(
            @ApiParam(name = "payload", value = "Payload", required = true)
                    JwtUserPayloadView payloadView);

    @POST
    @Path("/plugin/create")
    @Consumes(APPLICATION_JSON)
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_PLUGIN')")
    @ApiOperation(value = "JWT access and refresh token request for plugin")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 201,
                    message = "If successful, this method returns a JWT access and refresh token in the response body.",
                    response = JwtTokenVO.class),
            @ApiResponse(code = 404, message = "If access token not found")
    })
    Response pluginTokenRequest(
            @ApiParam(name = "payload", value = "Payload", required = true)
                    JwtPluginPayload payload);

    @POST
    @Path("/refresh")
    @Consumes(APPLICATION_JSON)
    @PreAuthorize("permitAll")
    @ApiOperation(value = "JWT access token request with refresh token")
    @ApiResponses({
            @ApiResponse(code = 201,
                    message = "If successful, this method returns a JWT access token in the response body.",
                    response = JwtAccessTokenVO.class),
            @ApiResponse(code = 404, message = "If access token not found")
    })
    Response refreshTokenRequest(
            @ApiParam(name = "refreshToken", value = "Refresh token", required = true)
                    JwtRefreshTokenVO jwtTokenVO);

    @POST
    @PreAuthorize("permitAll")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Login", notes = "Authenticates a user and returns a session-level JWT token.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "If successful, this method returns the object with the following properties in the response body.",
                    response = JwtTokenVO.class),
            @ApiResponse(code = 403, message = "If identity provider is not allowed")
    })
    Response login(
            @ApiParam(value = "Access key request", required = true)
                    JwtRequestVO request);

    @GET
    @Path("/plugin/authenticate")
    @PreAuthorize("permitAll")
    @ApiOperation(value = "Plugin authentication", notes = "Authenticates a plugin and JWT Plugin payload.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "If successful, this method returns the JwtPluginPayload.",
                    response = JwtPluginPayload.class),
            @ApiResponse(code = 401, message = "If authentication is not allowed")
    })
    Response authenticatePlugin(
            @ApiParam(name = "token", value = "Jwt Plugin Token", required = true)
            @QueryParam("token")
            String jwtPluginToken);
}


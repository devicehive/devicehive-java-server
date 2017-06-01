package com.devicehive.resource;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

import com.devicehive.vo.ConfigurationVO;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * Provide API information
 */
@Api(tags = {"Configuration"}, description = "Configuration operations", consumes="application/json")
@Path("/configuration")
public interface ConfigurationResource {

    @GET
    @Path("/{name}")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_CONFIGURATION')")
    @ApiOperation(value = "Get property", notes = "Returns requested property value")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(
                    code = 201,
                    message = "If successful, this method returns a Configuration resource in the response body.",
                    response = ConfigurationVO.class
            ),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    Response get(
            @ApiParam(name = "name", required = true, value = "Property name")
            @PathParam("name")
                    String name);

    @POST
    @Path("/{name}")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_CONFIGURATION')")
    @ApiOperation(value = "Create or update property", notes = "Creates new or updates existing property")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    Response setProperty(
            @ApiParam(name = "name", required = true, value = "Property name")
            @PathParam("name")
                    String name,
            @ApiParam(value = "Property value", required = true)
                    String value);

    @DELETE
    @Path("/{name}")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_CONFIGURATION')")
    @ApiOperation(value = "Delete property", notes = "Deletes property")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    Response deleteProperty(
            @ApiParam(name = "name", required = true, value = "Property name")
            @PathParam("name")
                    String name);
}

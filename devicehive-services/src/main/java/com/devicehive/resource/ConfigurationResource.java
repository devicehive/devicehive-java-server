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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
    @PreAuthorize("hasRole('ADMIN')")
    @Path("/{name}")
    @ApiOperation(value = "Get property", notes = "Returns requested property value")
    Response get(
            @ApiParam(name = "name", required = true, value = "Property name")
            @PathParam("name")
            String name);

    @PUT
    @PreAuthorize("hasRole('ADMIN')")
    @Path("/{name}")
    @ApiOperation(value = "Create or update property", notes = "Creates new or updates existing property")
    Response setProperty(
            @ApiParam(name = "name", required = true, value = "Property name")
            @PathParam("name")
            String name,
            @ApiParam(value = "Property value", required = true)
            String value);

    @GET
    @PreAuthorize("hasRole('ADMIN')")
    @Path("/{name}/set")
    @ApiOperation(value = "Create or update property", notes = "Creates new or updates existing property")
    Response setPropertyGet(
            @ApiParam(name = "name", required = true, value = "Property name")
            @PathParam("name")
            String name,
            @ApiParam(name = "value", value = "Property value", required = true)
            @QueryParam("value")
            String value);

    @DELETE
    @PreAuthorize("hasRole('ADMIN')")
    @Path("/{name}")
    @ApiOperation(value = "Delete property", notes = "Deletes property")
    Response deleteProperty(
            @ApiParam(name = "name", required = true, value = "Property name")
            @PathParam("name")
            String name);
}

package com.devicehive.resource;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * Provide API information
 */
@Api(tags = {"configuration"}, description = "Configuration operations")
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

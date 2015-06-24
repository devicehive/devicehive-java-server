package com.devicehive.resource;

import com.devicehive.auth.WwwAuthenticateRequired;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * Provide API information
 */
public interface ConfigurationResource {

    @GET
    @PreAuthorize("hasRole('ADMIN')")
    @Path("/{name}")
    Response get(@PathParam("name") String name);

    @PUT
    @PreAuthorize("hasRole('ADMIN')")
    @Path("/{name}")
    @WwwAuthenticateRequired
    Response setProperty(
            @PathParam("name") String name,
            String value);

    @GET
    @PreAuthorize("hasRole('ADMIN')")
    @Path("/{name}/set")
    @WwwAuthenticateRequired
    Response setPropertyGet(
            @PathParam("name")
            String name,
            @QueryParam("value")
            String value);

    @DELETE
    @PreAuthorize("hasRole('ADMIN')")
    @Path("/{name}")
    @WwwAuthenticateRequired
    Response deleteProperty(@PathParam("name") String name);
}

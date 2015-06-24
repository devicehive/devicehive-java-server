package com.devicehive.resource;

import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.WwwAuthenticateRequired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

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
    @RolesAllowed(HiveRoles.ADMIN)
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

    @POST
    @PreAuthorize("hasRole('ADMIN')")
    @Path("/auto")
    @WwwAuthenticateRequired
    Response auto(@HeaderParam(HttpHeaders.REFERER) String referer, @Context UriInfo uriInfo);

}

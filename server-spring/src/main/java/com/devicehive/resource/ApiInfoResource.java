package com.devicehive.resource;

import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

public interface ApiInfoResource {

    @GET
    @PreAuthorize("permitAll")
    Response getApiInfo(@Context UriInfo uriInfo);

    @GET
    @Path("/config/auth")
    @PreAuthorize("permitAll")
    Response getOauth2Config();

    @GET
    @Path("/config/cluster")
    @PreAuthorize("permitAll")
    Response getClusterConfig();
}

package com.devicehive.resource;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Api(tags = {"info"}, description = "API information")
@Path("/info")
public interface ApiInfoResource {

    @GET
    @PreAuthorize("permitAll")
    @ApiOperation(value = "Get API info", notes = "Returns version of API, server timestamp and WebSocket base uri")
    Response getApiInfo(@Context UriInfo uriInfo);

    @GET
    @Path("/config/auth")
    @PreAuthorize("permitAll")
    @ApiOperation(value = "Get oAuth configuration", notes = "Returns configured identity providers")
    Response getOauth2Config();

    @GET
    @Path("/config/cluster")
    @PreAuthorize("permitAll")
    @ApiOperation(value = "Get cluster configuration", notes = "Returns information about cluster (Kafka, Zookeeper etc.)")
    Response getClusterConfig();
}

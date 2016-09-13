package com.devicehive.resource;

import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/")
@Produces({"text/plain"})
public interface WelcomeResource {

    @GET
    @PreAuthorize("permitAll")
    Response getWelcomeInfo();
}

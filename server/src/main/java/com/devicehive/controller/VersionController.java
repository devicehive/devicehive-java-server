package com.devicehive.controller;

import com.devicehive.util.LogExecutionTime;

import javax.annotation.security.PermitAll;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Properties;

/**
 * Provides build information
 */
@Path("/version")
@LogExecutionTime
public class VersionController {

    @Context
    private ServletContext context;

    @GET
    @PermitAll
    public Response getVersionInfo() {
        Properties properties = new Properties();
        try {
            properties.load(context.getResourceAsStream("/WEB-INF/classes/app.properties"));
            return Response.ok(properties.getProperty("version")).build();
        } catch (IOException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}

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

import static com.devicehive.configuration.Constants.VERSION;

/**
 * Provides build information
 */
@Path("/version")
@LogExecutionTime
public class VersionController {
    private static final String propertiesPath = "/WEB-INF/classes/app.properties";
    @Context
    private ServletContext context;

    @GET
    @PermitAll
    public Response getVersionInfo() {
        Properties properties = new Properties();
        try {
            properties.load(context.getResourceAsStream(propertiesPath));
            return Response.ok(properties.getProperty(VERSION)).build();
        } catch (IOException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}

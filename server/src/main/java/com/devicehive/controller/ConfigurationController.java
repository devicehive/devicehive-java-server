package com.devicehive.controller;

import com.devicehive.auth.Authorized;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.util.LogExecutionTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static com.devicehive.configuration.Constants.NAME;
import static com.devicehive.configuration.Constants.VALUE;

/**
 * Provide API information
 */
@Path("/configuration")
@LogExecutionTime
public class ConfigurationController {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationController.class);
    @EJB
    private ConfigurationService configurationService;


    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/{" + NAME + "}")
    public Response get(@PathParam(NAME) String name) {
        return Response.ok().entity(configurationService.get(name)).build();
    }

    @PUT
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/{" + NAME + "}")
    public Response setProperty(@PathParam(NAME) String name, String value) {
        configurationService.save(name, value);
        return Response.ok().build();
    }

    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/{" + NAME + "}/set")
    public Response setPropertyGet(@PathParam(NAME) String name, @QueryParam(VALUE) String value) {
        configurationService.save(name, value);
        return Response.ok().build();
    }

    @DELETE
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/{" + NAME + "}")
    public Response deleteProperty(@PathParam(NAME) String name) {
        configurationService.delete(name);
        return Response.noContent().build();
    }

}

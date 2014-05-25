package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.util.LogExecutionTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static com.devicehive.configuration.Constants.NAME;
import static com.devicehive.configuration.Constants.VALUE;

/**
 * Provide API information
 */
@LogExecutionTime
@Path("/configuration")
public class ConfigurationController {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationController.class);

    @EJB
    private ConfigurationService configurationService;


    @POST
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/{"+NAME+"}")
    public void setProperty(@PathParam(NAME) String name, String value) {
        configurationService.save(name, value);
    }

    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/{"+NAME+"}")
    public void setPropertyGet(@PathParam(NAME) String name, @QueryParam(VALUE) String value) {
        configurationService.save(name, value);
    }

    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/{"+NAME+"}")
    public String get(@PathParam(NAME) String name) {
        return configurationService.get(name);
    }


    @DELETE
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/{"+NAME+"}")
    public Response deleteProperty(@PathParam(NAME) String name) {

        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }

    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/reload")
    public Response reloadConfig() {
        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }
}

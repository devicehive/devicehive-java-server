package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.util.LogExecutionTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
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
    @Path("/set/{" + NAME + "}")
    public Response setProperty(@PathParam(NAME) String name, String value) {
        configurationService.save(name, value);
        return Response.ok().build();
    }

    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/set/{" + NAME + "}")
    public Response setPropertyGet(@PathParam(NAME) String name, @QueryParam(VALUE) String value) {
        configurationService.save(name, value);
        return Response.ok().build();
    }

    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/{" + NAME + "}")
    public String get(@PathParam(NAME) String name) {
        return configurationService.get(name);
    }

    @DELETE
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/{" + NAME + "}")
    public Response deleteProperty(@PathParam(NAME) String name) {
        //TODO
        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }

}

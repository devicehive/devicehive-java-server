package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.util.LogExecutionTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Provide API information
 */
@LogExecutionTime
@Path("/config")
public class ConfigurationController {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationController.class);
    private ConfigurationService configurationService;

    @EJB
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @POST
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/set")
    public Response setPropertyPost(@QueryParam("name") @NotNull String name, @QueryParam("value") String value) {
        return setProperty(name, value);
    }

    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/set")
    public Response setPropertyGet(@QueryParam("name") @NotNull String name, @QueryParam("value") String value) {
        return setProperty(name, value);
    }

    private Response setProperty(String name, String value) {
        logger.debug("Configuration will be set. Property's name : {} value : {} ", name, value);
        configurationService.save(name, value);
        logger.debug("Configuration has been set. Property's name : {} value : {} ", name, value);
        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }

    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/reload")
    public Response reloadConfig() {
        configurationService.notifyUpdateAll();
        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }
}

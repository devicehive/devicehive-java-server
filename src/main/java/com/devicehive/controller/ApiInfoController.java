package com.devicehive.controller;

import com.devicehive.model.ApiInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provide API information
 */
@Path("/info")
public class ApiInfoController {
    private static final Logger logger = LoggerFactory.getLogger(ApiInfoController.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ApiInfo getApiInfo() {
        logger.debug("ApiInfo requested");

        return new ApiInfo();
    }
}

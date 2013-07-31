package com.devicehive.controller;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_SERVER_INFO;

import java.sql.Timestamp;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;

import com.devicehive.json.strategies.JsonPolicyDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.ApiInfo;
import com.devicehive.model.Version;
import com.devicehive.service.UserService;

/**
 * Provide API information
 */
@Path("/info")
public class ApiInfoController {
    private static final Logger logger = LoggerFactory.getLogger(ApiInfoController.class);

    @EJB
    private UserService userService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getApiInfo() {
        logger.debug("ApiInfo requested");
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Version.VERSION);
        apiInfo.setServerTimestamp(new Timestamp(System.currentTimeMillis()));
        return ResponseFactory.response(Response.Status.OK, apiInfo, JsonPolicyDef.Policy.REST_SERVER_INFO);
    }
}

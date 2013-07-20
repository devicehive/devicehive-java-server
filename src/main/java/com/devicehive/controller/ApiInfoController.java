package com.devicehive.controller;

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.ApiInfo;
import com.devicehive.model.Version;
import com.devicehive.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.sql.Timestamp;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_SERVER_INFO;

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
    @JsonPolicyApply(REST_SERVER_INFO)
    @PermitAll
    public ApiInfo getApiInfo() {
        logger.debug("ApiInfo requested");
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Version.VERSION);
        apiInfo.setServerTimestamp(new Timestamp(System.currentTimeMillis()));
        return apiInfo;
    }
}

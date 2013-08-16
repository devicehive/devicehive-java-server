package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.ConfigurationDAO;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ApiInfo;
import com.devicehive.model.Configuration;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.Version;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.service.TimestampService;
import com.devicehive.utils.LogExecutionTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provide API information
 */
@LogExecutionTime
@Path("/info")
public class ApiInfoController {
    private static final Logger logger = LoggerFactory.getLogger(ApiInfoController.class);

    @EJB
    private TimestampService timestampService;

    @EJB
    private ConfigurationService configurationService;

    @GET
    @PermitAll
    public Response getApiInfo() {
        logger.debug("ApiInfo requested");
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Version.VERSION);
        apiInfo.setServerTimestamp(timestampService.getTimestamp());
        String url = configurationService.get(Constants.WEBSOCKET_SERVER_URL);
        if (url != null) {
            apiInfo.setWebSocketServerUrl(url);
        }
        return ResponseFactory.response(Response.Status.OK, apiInfo, JsonPolicyDef.Policy.REST_SERVER_INFO);
    }

}

package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.ConfigurationDAO;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ApiInfo;
import com.devicehive.model.Configuration;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.Version;
import com.devicehive.service.ConfigurationService;
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
@Path("/")
public class ApiInfoController {
    private static final Logger logger = LoggerFactory.getLogger(ApiInfoController.class);

    @EJB
    private TimestampService timestampService;

    @EJB
    private ConfigurationDAO configurationDAO;

    @EJB
    private ConfigurationService configurationService;

    @GET
    @PermitAll
    @Path("info")
    public Response getApiInfo() {
        logger.debug("ApiInfo requested");
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Version.VERSION);
        apiInfo.setServerTimestamp(timestampService.getTimestamp());
        Configuration url = configurationDAO.findByName(Constants.WEBSOCKET_SERVER_URL);
        if (url != null) {
            apiInfo.setWebSocketServerUrl(url.getValue());
        }
        return ResponseFactory.response(Response.Status.OK, apiInfo, JsonPolicyDef.Policy.REST_SERVER_INFO);
    }

    @PUT
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("config")
    public Response setProperty(Configuration configuration) {
        if (configuration == null) {
            logger.debug("Unable to set configuration. Bad request. configuration = " + configuration);
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        }
        logger.debug("Congiguration will be set. Property's name : " + configuration.getName() + ", " +
                "value : " + configuration.getValue());
        configurationService.save(configuration.getName(), configuration.getValue());
        logger.debug("Congiguration has been set. Property's name : " + configuration.getName() + ", " +
                "value : " + configuration.getValue());
        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }
}

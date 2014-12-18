package com.devicehive.controller;


import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ApiConfig;
import com.devicehive.model.ApiInfo;
import com.devicehive.model.IdentityProviderConfig;
import com.devicehive.service.TimestampService;
import com.devicehive.util.LogExecutionTime;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Provide API information
 */
@Path("/info")
@LogExecutionTime
public class ApiInfoController {

    private static final Logger logger = LoggerFactory.getLogger(ApiInfoController.class);

    @EJB
    private TimestampService timestampService;

    @EJB
    private ConfigurationService configurationService;

    @EJB
    private PropertiesService propertiesService;

    @GET
    @PermitAll
    public Response getApiInfo() {
        logger.debug("ApiInfo requested");
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Constants.API_VERSION);
        apiInfo.setServerTimestamp(timestampService.getTimestamp());
        String url = configurationService.get(Constants.WEBSOCKET_SERVER_URL);
        if (url != null) {
            apiInfo.setWebSocketServerUrl(url);
        }
        return ResponseFactory.response(Response.Status.OK, apiInfo, JsonPolicyDef.Policy.REST_SERVER_INFO);
    }

    @GET
    @Path("/config/oauth2")
    @PermitAll
    public Response getOauth2Config() {
        logger.debug("ApiConfig requested");
        ApiConfig apiConfig = new ApiConfig();

        final String googleClientId = configurationService.get(Constants.GOOGLE_IDENTITY_CLIENT_ID);
        final String facebookClientId = configurationService.get(Constants.FACEBOOK_IDENTITY_CLIENT_ID);
        final String githubClientId = configurationService.get(Constants.GITHUB_IDENTITY_CLIENT_ID);

        if (!StringUtils.isBlank(googleClientId)) {
            IdentityProviderConfig googleConfig = new IdentityProviderConfig();
            googleConfig.setClientId(googleClientId);
            googleConfig.setProviderId(Long.parseLong(propertiesService.getProperty(Constants.GOOGLE_IDENTITY_PROVIDER_ID)));
            googleConfig.setIsAvailable(Boolean.parseBoolean(configurationService.get(Constants.GOOGLE_IDENTITY_ALLOWED)));
            apiConfig.setGoogle(googleConfig);
        }

        if (!StringUtils.isBlank(facebookClientId)) {
            IdentityProviderConfig facebookConfig = new IdentityProviderConfig();
            facebookConfig.setClientId(facebookClientId);
            facebookConfig.setProviderId(Long.parseLong(propertiesService.getProperty(Constants.FACEBOOK_IDENTITY_PROVIDER_ID)));
            facebookConfig.setIsAvailable(Boolean.parseBoolean(configurationService.get(Constants.FACEBOOK_IDENTITY_ALLOWED)));
            apiConfig.setFacebook(facebookConfig);
        }

        if (!StringUtils.isBlank(githubClientId)) {
            IdentityProviderConfig githubConfig = new IdentityProviderConfig();
            githubConfig.setClientId(githubClientId);
            githubConfig.setProviderId(Long.parseLong(propertiesService.getProperty(Constants.GITHUB_IDENTITY_PROVIDER_ID)));
            githubConfig.setIsAvailable(Boolean.parseBoolean(configurationService.get(Constants.GITHUB_IDENTITY_ALLOWED)));
            apiConfig.setGithub(githubConfig);
        }

        return ResponseFactory.response(Response.Status.OK, apiConfig, JsonPolicyDef.Policy.REST_SERVER_CONFIG);
    }

}

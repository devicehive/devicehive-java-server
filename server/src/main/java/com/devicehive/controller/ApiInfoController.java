package com.devicehive.controller;


import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ApiConfig;
import com.devicehive.model.ApiInfo;
import com.devicehive.model.ClusterConfig;
import com.devicehive.model.IdentityProviderConfig;
import com.devicehive.service.TimestampService;
import com.devicehive.util.LogExecutionTime;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

/**
 * Provide API information
 */
@Path("/info")
@LogExecutionTime
public class ApiInfoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiInfoController.class);

    @EJB
    private TimestampService timestampService;

    @EJB
    private ConfigurationService configurationService;

    @GET
    @PermitAll
    public Response getApiInfo() {
        LOGGER.debug("ApiInfo requested");
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
    @Path("/config/auth")
    @PermitAll
    public Response getOauth2Config() {
        LOGGER.debug("ApiConfig requested");
        ApiConfig apiConfig = new ApiConfig();

        Set<IdentityProviderConfig> providerConfigs = new HashSet<>();

        if (Boolean.parseBoolean(configurationService.get(Constants.GOOGLE_IDENTITY_ALLOWED))) {
            IdentityProviderConfig googleConfig = new IdentityProviderConfig("google");
            googleConfig.setClientId(configurationService.get(Constants.GOOGLE_IDENTITY_CLIENT_ID));
            providerConfigs.add(googleConfig);
        }

        if (Boolean.parseBoolean(configurationService.get(Constants.FACEBOOK_IDENTITY_ALLOWED))) {
            IdentityProviderConfig facebookConfig = new IdentityProviderConfig("facebook");
            facebookConfig.setClientId(configurationService.get(Constants.FACEBOOK_IDENTITY_CLIENT_ID));
            providerConfigs.add(facebookConfig);
        }

        if (Boolean.parseBoolean(configurationService.get(Constants.GITHUB_IDENTITY_ALLOWED))) {
            IdentityProviderConfig githubConfig = new IdentityProviderConfig("github");
            githubConfig.setClientId(configurationService.get(Constants.GITHUB_IDENTITY_CLIENT_ID));
            providerConfigs.add(githubConfig);
        }

        IdentityProviderConfig passwordConfig = new IdentityProviderConfig("password");
        passwordConfig.setClientId("");
        providerConfigs.add(passwordConfig);

        apiConfig.setProviderConfigs(providerConfigs);
        apiConfig.setSessionTimeout(Long.parseLong(configurationService.get(Constants.SESSION_TIMEOUT)) / 1000);

        return ResponseFactory.response(Response.Status.OK, apiConfig, JsonPolicyDef.Policy.REST_SERVER_CONFIG);
    }

    @GET
    @Path("/config/cluster")
    @PermitAll
    public Response getClusterConfig() {
        LOGGER.debug("ClusterConfig requested");
        ClusterConfig clusterConfig = new ClusterConfig();
        clusterConfig.setMetadataBrokerList(configurationService.get(Constants.METADATA_BROKER_LIST));
        clusterConfig.setZookeeperConnect(configurationService.get(Constants.ZOOKEEPER_CONNECT));
        final String cassandraContactpoints = configurationService.get(Constants.CASSANDRA_CONTACTPOINTS);
        if (StringUtils.isNotBlank(cassandraContactpoints)) {
            clusterConfig.setCassandraContactpoints(cassandraContactpoints);
        }
        final String threadCount = configurationService.get(Constants.THREADS_COUNT);
        if (StringUtils.isNotBlank(threadCount) && NumberUtils.isNumber(threadCount)) {
            clusterConfig.setThreadsCount(Integer.parseInt(threadCount));
        } else {
            clusterConfig.setThreadsCount(1);
        }
        return ResponseFactory.response(Response.Status.OK, clusterConfig, JsonPolicyDef.Policy.REST_CLUSTER_CONFIG);
    }

}

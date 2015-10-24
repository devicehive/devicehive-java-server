package com.devicehive.resource.impl;


import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ApiConfig;
import com.devicehive.model.ApiInfo;
import com.devicehive.model.ClusterConfig;
import com.devicehive.model.IdentityProviderConfig;
import com.devicehive.resource.ApiInfoResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.time.TimestampService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashSet;
import java.util.Set;

/**
 * Provide API information
 */
@Service
public class ApiInfoResourceImpl implements ApiInfoResource {
    private static final Logger logger = LoggerFactory.getLogger(ApiInfoResourceImpl.class);

    @Autowired
    private TimestampService timestampService;
    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private Environment env;

    @Value("${server.context-path}")
    private String contextPath;

    @Override
    public Response getApiInfo(UriInfo uriInfo) {
        logger.debug("ApiInfo requested");
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Constants.class.getPackage().getImplementationVersion());
        apiInfo.setServerTimestamp(timestampService.getTimestamp());
        
        // Generate websocket url based on current request url
        int port = uriInfo.getBaseUri().getPort();
        if (port == -1) {
            apiInfo.setWebSocketServerUrl("ws://" + uriInfo.getBaseUri().getHost() + contextPath + "/websocket");
        } else {
            apiInfo.setWebSocketServerUrl("ws://" + uriInfo.getBaseUri().getHost() + ":" + uriInfo.getBaseUri().getPort() + contextPath + "/websocket");
        }
        
        return ResponseFactory.response(Response.Status.OK, apiInfo, JsonPolicyDef.Policy.REST_SERVER_INFO);
    }

    @Override
    public Response getOauth2Config() {
        logger.debug("ApiConfig requested");
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

    @Override
    public Response getClusterConfig() {
        logger.debug("ClusterConfig requested");
        ClusterConfig clusterConfig = new ClusterConfig();
        clusterConfig.setMetadataBrokerList(env.getProperty(Constants.METADATA_BROKER_LIST));
        clusterConfig.setZookeeperConnect(env.getProperty(Constants.ZOOKEEPER_CONNECT));

        final String threadCount = env.getProperty(Constants.THREADS_COUNT);
        if (StringUtils.isNotBlank(threadCount) && NumberUtils.isNumber(threadCount)) {
            clusterConfig.setThreadsCount(Integer.parseInt(threadCount));
        } else {
            clusterConfig.setThreadsCount(1);
        }
        return ResponseFactory.response(Response.Status.OK, clusterConfig, JsonPolicyDef.Policy.REST_CLUSTER_CONFIG);
    }

}

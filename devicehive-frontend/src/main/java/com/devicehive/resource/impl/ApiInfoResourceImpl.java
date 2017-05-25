package com.devicehive.resource.impl;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.resource.ApiInfoResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.ApiInfoVO;
import com.devicehive.vo.ClusterConfigVO;
import com.devicehive.vo.IdentityProviderConfig;
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

    @Value("${build.version}")
    private String appVersion;

    @Override
    public Response getApiInfo(UriInfo uriInfo) {
        logger.debug("ApiInfoVO requested");
        ApiInfoVO apiInfo = new ApiInfoVO();
        String version = Constants.class.getPackage().getImplementationVersion();

        if(version == null) {
            apiInfo.setApiVersion(appVersion);
        } else {
            apiInfo.setApiVersion(version);
        }
        apiInfo.setServerTimestamp(timestampService.getDate());
        
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
    public Response getClusterConfig() {
        logger.debug("ClusterConfigVO requested");
        ClusterConfigVO clusterConfig = new ClusterConfigVO();
        clusterConfig.setBootstrapServers(env.getProperty(Constants.BOOTSTRAP_SERVERS));
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

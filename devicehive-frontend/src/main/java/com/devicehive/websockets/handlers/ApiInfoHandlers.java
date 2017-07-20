package com.devicehive.websockets.handlers;

/*
 * #%L
 * DeviceHive Frontend Logic
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

import com.devicehive.configuration.Constants;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.ApiInfoVO;
import com.devicehive.vo.ClusterConfigVO;
import com.devicehive.websockets.converters.WebSocketResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.devicehive.configuration.Constants.BOOTSTRAP_SERVERS;
import static com.devicehive.configuration.Constants.CLUSTER_INFO;
import static com.devicehive.configuration.Constants.INFO;
import static com.devicehive.configuration.Constants.ZOOKEEPER_CONNECT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_CLUSTER_CONFIG;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.WEBSOCKET_SERVER_INFO;

@Component
public class ApiInfoHandlers {

    private static final Logger logger = LoggerFactory.getLogger(ApiInfoHandlers.class);

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private Environment env;

    @Value("${server.context-path}")
    private String contextPath;


    @PreAuthorize("permitAll")
    public WebSocketResponse processServerInfo(WebSocketSession session) {
        logger.debug("server/info action started. Session " + session.getId());
        ApiInfoVO apiInfo = new ApiInfoVO();
        apiInfo.setApiVersion(Constants.class.getPackage().getImplementationVersion());
        session.getHandshakeHeaders().get("Host").stream()
                .findFirst()
                .ifPresent(host -> apiInfo.setRestServerUrl("http://" + host + contextPath + "/rest"));

        //TODO: Replace with timestamp service
        apiInfo.setServerTimestamp(timestampService.getDate());
        WebSocketResponse response = new WebSocketResponse();
        response.addValue(INFO, apiInfo, WEBSOCKET_SERVER_INFO);
        logger.debug("server/info action completed. Session {}", session.getId());
        return response;
    }

    @PreAuthorize("permitAll")
    public WebSocketResponse processClusterConfigInfo(WebSocketSession session) {
        logger.debug("cluster/info action started. Session " + session.getId());
        ClusterConfigVO clusterConfig = new ClusterConfigVO();
        clusterConfig.setBootstrapServers(env.getProperty(BOOTSTRAP_SERVERS));
        clusterConfig.setZookeeperConnect(env.getProperty(ZOOKEEPER_CONNECT));

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(CLUSTER_INFO, clusterConfig, REST_CLUSTER_CONFIG);
        return response;
    }

}

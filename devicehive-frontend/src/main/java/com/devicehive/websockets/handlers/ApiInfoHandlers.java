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

import com.devicehive.auth.websockets.HiveWebsocketAuth;
import com.devicehive.configuration.Constants;
import com.devicehive.messages.handler.WebSocketClientHandler;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.ApiInfoVO;
import com.devicehive.vo.CacheInfoVO;
import com.devicehive.vo.ClusterConfigVO;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.JsonObject;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.devicehive.configuration.Constants.BOOTSTRAP_SERVERS;
import static com.devicehive.configuration.Constants.CACHE_INFO;
import static com.devicehive.configuration.Constants.CLUSTER_INFO;
import static com.devicehive.configuration.Constants.INFO;
import static com.devicehive.configuration.Constants.ZOOKEEPER_CONNECT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_CLUSTER_CONFIG;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.WEBSOCKET_SERVER_INFO;

@Component
public class ApiInfoHandlers {

    private static final Logger logger = LoggerFactory.getLogger(ApiInfoHandlers.class);

    protected final TimestampService timestampService;
    protected final WebSocketClientHandler clientHandler;
    private final Environment env;
    private final LocalContainerEntityManagerFactoryBean entityManagerFactory;

    @Value("${server.context-path}")
    private String contextPath;
    
    @Autowired
    public ApiInfoHandlers(TimestampService timestampService,
            Environment env,
            WebSocketClientHandler clientHandler,
            LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        this.timestampService = timestampService;
        this.clientHandler = clientHandler;
        this.env = env;
        this.entityManagerFactory = entityManagerFactory;
    }

    @HiveWebsocketAuth
    @PreAuthorize("permitAll")
    public void processServerInfo(JsonObject request, WebSocketSession session) {
        logger.debug("server/info action started. Session " + session.getId());
        ApiInfoVO apiInfo = new ApiInfoVO();
        apiInfo.setApiVersion(Constants.class.getPackage().getImplementationVersion());
        session.getHandshakeHeaders().get("Host").stream()
                .findFirst()
                .ifPresent(host -> apiInfo.setRestServerUrl("http://" + host + contextPath + "/rest"));

        apiInfo.setServerTimestamp(timestampService.getDate());
        WebSocketResponse response = new WebSocketResponse();
        response.addValue(INFO, apiInfo, WEBSOCKET_SERVER_INFO);
        logger.debug("server/info action completed. Session {}", session.getId());
        clientHandler.sendMessage(request, response, session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("permitAll")
    public void processServerCacheInfo(JsonObject request, WebSocketSession session) {
        logger.debug("server/cacheInfo action started. Session " + session.getId());
        CacheInfoVO cacheInfo = new CacheInfoVO();
        cacheInfo.setServerTimestamp(timestampService.getDate());
        cacheInfo.setCacheStats(getCacheStats());
        WebSocketResponse response = new WebSocketResponse();
        response.addValue(CACHE_INFO, cacheInfo, WEBSOCKET_SERVER_INFO);
        logger.debug("server/cacheI action completed. Session {}", session.getId());
        clientHandler.sendMessage(request, response, session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("permitAll")
    public void processClusterConfigInfo(JsonObject request, WebSocketSession session) {
        logger.debug("cluster/info action started. Session " + session.getId());
        ClusterConfigVO clusterConfig = new ClusterConfigVO();
        clusterConfig.setBootstrapServers(env.getProperty(BOOTSTRAP_SERVERS));
        clusterConfig.setZookeeperConnect(env.getProperty(ZOOKEEPER_CONNECT));

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(CLUSTER_INFO, clusterConfig, REST_CLUSTER_CONFIG);
        clientHandler.sendMessage(request, response, session);
    }

    private String getCacheStats() {
        SessionFactory sessionFactory = entityManagerFactory.getNativeEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();

        return statistics.toString();
    }

}

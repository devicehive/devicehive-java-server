package com.devicehive.service;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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


import com.devicehive.exceptions.HiveException;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.rpc.PluginSubscribeRequest;
import com.devicehive.model.updates.PluginUpdate;
import com.devicehive.proxy.config.WebSocketKafkaProxyConfig;
import com.devicehive.security.jwt.JwtPluginPayload;
import com.devicehive.service.helpers.HttpRestHelper;
import com.devicehive.service.helpers.LongIdGenerator;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.kafka.topic.KafkaTopicService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.JwtTokenVO;
import com.devicehive.vo.PluginVO;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.ServiceUnavailableException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

@Component
public class PluginRegisterService {
    private static final Logger logger = LoggerFactory.getLogger(PluginRegisterService.class);

    @Value("${auth.base.url}")
    private String authBaseUrl;
    
    private final HiveValidator hiveValidator;
    private final PluginService pluginService;
    private final RpcClient rpcClient;
    private final KafkaTopicService kafkaTopicService;
    private final LongIdGenerator idGenerator;
    private final BaseDeviceService deviceService;
    private final HttpRestHelper httpRestHelper;
    private final WebSocketKafkaProxyConfig webSocketKafkaProxyConfig;
    private final Gson gson;

    @Autowired
    public PluginRegisterService(
            HiveValidator hiveValidator,
            PluginService pluginService,
            RpcClient rpcClient,
            KafkaTopicService kafkaTopicService,
            LongIdGenerator idGenerator,
            BaseDeviceService deviceService,
            HttpRestHelper httpRestHelper,
            WebSocketKafkaProxyConfig webSocketKafkaProxyConfig,
            Gson gson) {
        this.hiveValidator = hiveValidator;
        this.pluginService = pluginService;
        this.rpcClient = rpcClient;
        this.kafkaTopicService = kafkaTopicService;
        this.idGenerator = idGenerator;
        this.deviceService = deviceService;
        this.httpRestHelper = httpRestHelper;
        this.webSocketKafkaProxyConfig = webSocketKafkaProxyConfig;
        this.gson = gson;
    }

    @Transactional
    public CompletableFuture<JsonObject> register(PluginSubscribeRequest pollRequest, PluginUpdate pluginUpdate,
            String authorization) {

        return persistPlugin(pollRequest, pluginUpdate).thenApply(pluginVO -> {
            JwtTokenVO jwtTokenVO = createPluginTokens(pluginVO.getTopicName(), authorization);
            JsonObject response = new JsonObject();

            response.addProperty("accessToken", jwtTokenVO.getAccessToken());
            response.addProperty("refreshToken", jwtTokenVO.getRefreshToken());
            response.addProperty("proxyEndpoint", webSocketKafkaProxyConfig.getProxyConnect());

            return response;
        });
    }

    private CompletableFuture<PluginVO> persistPlugin(PluginSubscribeRequest pollRequest, PluginUpdate pluginUpdate) {
        hiveValidator.validate(pluginUpdate);
        PluginVO pluginVO = pluginUpdate.convertTo();
        pluginVO.setUserId(pollRequest.getFilter().getPrincipal().getUser().getId());

        //Creation of topic for plugin
        String pluginTopic = "plugin_topic_" + UUID.randomUUID().toString();
        kafkaTopicService.createTopic(pluginTopic);
        pluginVO.setTopicName(pluginTopic);

        //Creation of subscription for plugin
        final Long subscriptionId = idGenerator.generate();
        pollRequest.setSubscriptionId(subscriptionId);
        pollRequest.setTopicName(pluginTopic);

        pluginVO.setSubscriptionId(subscriptionId);
        pluginService.create(pluginVO);

        //Update deviceIds in Filter taking into account networkIds and permissions
        Filter filter = pollRequest.getFilter();
        filter.setDeviceIds(deviceService.getAvailableDeviceIds(filter.getDeviceIds(), filter.getNetworkIds()));

        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(pollRequest)
                .build(), new ResponseConsumer(future));
        
        return future.thenApply(response -> pluginVO);
    }

    private JwtTokenVO createPluginTokens(String topicName, String authorization) {
        JwtPluginPayload jwtPluginPayload = new JwtPluginPayload(topicName, null, null);
        
        JwtTokenVO jwtToken = null;
        try {
            jwtToken = httpRestHelper.post(authBaseUrl + "/token/plugin/create", gson.toJson(jwtPluginPayload), JwtTokenVO.class, authorization);
        } catch (ServiceUnavailableException e) {
            throw new HiveException(e.getMessage(), SC_SERVICE_UNAVAILABLE);
        }
        
        return jwtToken;

    }


}

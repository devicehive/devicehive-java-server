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


import com.devicehive.auth.HivePrincipal;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.FilterEntity;
import com.devicehive.model.enums.PluginStatus;
import com.devicehive.model.query.PluginReqisterQuery;
import com.devicehive.model.query.PluginUpdateQuery;
import com.devicehive.model.rpc.BasePluginRequest;
import com.devicehive.model.rpc.PluginSubscribeRequest;
import com.devicehive.model.rpc.PluginUnsubscribeRequest;
import com.devicehive.model.updates.PluginUpdate;
import com.devicehive.proxy.config.WebSocketKafkaProxyConfig;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.security.jwt.JwtPluginPayload;
import com.devicehive.service.helpers.HttpRestHelper;
import com.devicehive.service.helpers.LongIdGenerator;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.shim.api.Body;
import com.devicehive.shim.api.Request;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.devicehive.auth.HiveAction.MANAGE_PLUGIN;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.PLUGIN_SUBMITTED;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;

@Component
public class PluginRegisterService {
    private static final Logger logger = LoggerFactory.getLogger(PluginRegisterService.class);

    @Value("${auth.base.url}")
    private String authBaseUrl;
    
    private final HiveValidator hiveValidator;
    private final PluginService pluginService;
    private final BaseDeviceService deviceService;
    private final FilterService filterService;
    private final RpcClient rpcClient;
    private final KafkaTopicService kafkaTopicService;
    private final LongIdGenerator idGenerator;
    private final HttpRestHelper httpRestHelper;
    private final WebSocketKafkaProxyConfig webSocketKafkaProxyConfig;
    private final Gson gson;

    @Autowired
    public PluginRegisterService(
            HiveValidator hiveValidator,
            PluginService pluginService,
            BaseDeviceService deviceService, FilterService filterService, RpcClient rpcClient,
            KafkaTopicService kafkaTopicService,
            LongIdGenerator idGenerator,
            HttpRestHelper httpRestHelper,
            WebSocketKafkaProxyConfig webSocketKafkaProxyConfig,
            Gson gson) {
        this.hiveValidator = hiveValidator;
        this.pluginService = pluginService;
        this.deviceService = deviceService;
        this.filterService = filterService;
        this.rpcClient = rpcClient;
        this.kafkaTopicService = kafkaTopicService;
        this.idGenerator = idGenerator;
        this.httpRestHelper = httpRestHelper;
        this.webSocketKafkaProxyConfig = webSocketKafkaProxyConfig;
        this.gson = gson;
    }

    @Transactional
    public CompletableFuture<Response> register(Long userId, PluginReqisterQuery pluginReqisterQuery, PluginUpdate pluginUpdate,
                                                String authorization) {
        PluginSubscribeRequest pollRequest = pluginReqisterQuery.toRequest(filterService);

        return persistPlugin(pollRequest, pluginUpdate, pluginReqisterQuery.constructFilterString(), userId).thenApply(pluginVO -> {
            JwtTokenVO jwtTokenVO = createPluginTokens(pluginVO.getTopicName(), authorization);
            JsonObject response = createTokenResponse(pluginVO.getTopicName(), jwtTokenVO);

            return ResponseFactory.response(CREATED, response, PLUGIN_SUBMITTED);
        });
    }

    @Transactional
    public CompletableFuture<Response> update(String topicName, PluginUpdateQuery pluginUpdateQuery, String authorization) {
        return updatePlugin(topicName, pluginUpdateQuery).thenApply(plugin ->
            ResponseFactory.response(NO_CONTENT)
        );
    }

    @Transactional
    public CompletableFuture<Response> delete(String topicName, String authorization) {
        PluginVO existingPlugin = pluginService.findByTopic(topicName);
        if (existingPlugin == null) {
            throw new NotFoundException("Plugin with topic name " + topicName + " was not found");
        }

        pluginService.delete(existingPlugin.getId());

        PluginUnsubscribeRequest request = new PluginUnsubscribeRequest(existingPlugin.getSubscriptionId(), existingPlugin.getTopicName());
        CompletableFuture<com.devicehive.shim.api.Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(request)
                .build(), new ResponseConsumer(future));

        return future.thenApply(response -> ResponseFactory.response(NO_CONTENT));
    }

    private CompletableFuture<PluginVO> persistPlugin(PluginSubscribeRequest pollRequest, PluginUpdate pluginUpdate, String filterString, Long userId) {
        hiveValidator.validate(pluginUpdate);
        PluginVO pluginVO = pluginUpdate.convertTo();
        pluginVO.setUserId(userId);
        pluginVO.setFilter(filterString);
        pluginVO.setStatus(PluginStatus.CREATED);

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

        CompletableFuture<com.devicehive.shim.api.Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(pollRequest)
                .build(), new ResponseConsumer(future));
        
        return future.thenApply(response -> pluginVO);
    }

    private CompletableFuture<PluginVO> updatePlugin(String topicName, PluginUpdateQuery pluginUpdateQuery) {
        if (pluginUpdateQuery.getStatus()!= null && pluginUpdateQuery.getStatus().equals(PluginStatus.CREATED)) {
            throw new IllegalArgumentException("Cannot change status of existing plugin to Created.");
        }

        PluginVO existingPlugin = pluginService.findByTopic(topicName);
        if (existingPlugin == null) {
            throw new NotFoundException("Plugin with topic name " + topicName + " was not found");
        }

        if (pluginUpdateQuery.getName() != null) {
            existingPlugin.setName(pluginUpdateQuery.getName());
        }

        if (pluginUpdateQuery.getDescription() != null) {
            existingPlugin.setDescription(pluginUpdateQuery.getDescription());
        }

        if (pluginUpdateQuery.getParameters() != null) {
            existingPlugin.setParameters(pluginUpdateQuery.getParameters());
        }

        if (pluginUpdateQuery.getStatus() != null) {
            existingPlugin.setStatus(pluginUpdateQuery.getStatus());
        }

        // if no new information about filters is provided in PluginUpdateQuery, we should keep the same filters
        FilterEntity filterEntity = new FilterEntity(existingPlugin.getFilter());
        if (pluginUpdateQuery.getDeviceId() == null) {
            pluginUpdateQuery.setDeviceId(filterEntity.getDeviceId());
        }

        if (pluginUpdateQuery.getNetworkIds() == null) {
            pluginUpdateQuery.setNetworkIds(filterEntity.getNetworkIds());
        }

        if (pluginUpdateQuery.getDeviceTypeIds() == null) {
            pluginUpdateQuery.setDeviceTypeIds(filterEntity.getDeviceTypeIds());
        }

        if (pluginUpdateQuery.getNames() == null) {
            pluginUpdateQuery.setNames(filterEntity.getNames());
        }

        if (pluginUpdateQuery.isReturnCommands() == null) {
            pluginUpdateQuery.setReturnCommands(filterEntity.isReturnCommands());
        }

        if (pluginUpdateQuery.isReturnUpdatedCommands() == null) {
            pluginUpdateQuery.setReturnUpdatedCommands(filterEntity.isReturnUpdatedCommands());
        }

        if (pluginUpdateQuery.isReturnNotifications() == null) {
            pluginUpdateQuery.setReturnNotifications(filterEntity.isReturnNotifications());
        }

        existingPlugin.setFilter(pluginUpdateQuery.constructFilterString());

        pluginService.update(existingPlugin);

        CompletableFuture<com.devicehive.shim.api.Response> future = new CompletableFuture<>();

        BasePluginRequest request;
        if (existingPlugin.getStatus().equals(PluginStatus.ACTIVE)) {
            request = pluginUpdateQuery.toRequest(filterService);
            request.setSubscriptionId(existingPlugin.getSubscriptionId());
        } else {
            request = new PluginUnsubscribeRequest(existingPlugin.getSubscriptionId(), existingPlugin.getTopicName());
        }

        rpcClient.call(Request.newBuilder()
                .withBody(request)
                .build(), new ResponseConsumer(future));

        return future.thenApply(response -> existingPlugin);
    }

    private JwtTokenVO createPluginTokens(String topicName, String authorization) {
        JwtPluginPayload jwtPluginPayload = new JwtPluginPayload(Collections.singleton(MANAGE_PLUGIN.getId()), topicName, null, null);
        
        JwtTokenVO jwtToken = null;
        try {
            jwtToken = httpRestHelper.post(authBaseUrl + "/token/plugin/create", gson.toJson(jwtPluginPayload), JwtTokenVO.class, authorization);
        } catch (ServiceUnavailableException e) {
            logger.warn("Service is not available");
            throw new HiveException(e.getMessage(), SC_SERVICE_UNAVAILABLE);
        }
        
        return jwtToken;

    }

    private JsonObject createTokenResponse(String topicName, JwtTokenVO jwtTokenVO) {
        JsonObject response = new JsonObject();

        response.addProperty("accessToken", jwtTokenVO.getAccessToken());
        response.addProperty("refreshToken", jwtTokenVO.getRefreshToken());
        response.addProperty("proxyEndpoint", webSocketKafkaProxyConfig.getProxyConnect());
        response.addProperty("topicName", topicName);

        return response;
    }

}

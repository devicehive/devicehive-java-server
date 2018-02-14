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
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.FilterEntity;
import com.devicehive.model.enums.PluginStatus;
import com.devicehive.model.query.PluginReqisterQuery;
import com.devicehive.model.query.PluginUpdateQuery;
import com.devicehive.model.response.EntityCountResponse;
import com.devicehive.model.rpc.*;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.devicehive.auth.HiveAction.MANAGE_PLUGIN;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.PLUGIN_SUBMITTED;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static javax.ws.rs.core.Response.Status.*;

@Component
public class PluginRegisterService {
    private static final Logger logger = LoggerFactory.getLogger(PluginRegisterService.class);

    @Value("${auth.base.url}")
    private String authBaseUrl;
    
    private final HiveValidator hiveValidator;
    private final PluginService pluginService;
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
            FilterService filterService, RpcClient rpcClient,
            KafkaTopicService kafkaTopicService,
            LongIdGenerator idGenerator,
            HttpRestHelper httpRestHelper,
            WebSocketKafkaProxyConfig webSocketKafkaProxyConfig,
            Gson gson) {
        this.hiveValidator = hiveValidator;
        this.pluginService = pluginService;
        this.filterService = filterService;
        this.rpcClient = rpcClient;
        this.kafkaTopicService = kafkaTopicService;
        this.idGenerator = idGenerator;
        this.httpRestHelper = httpRestHelper;
        this.webSocketKafkaProxyConfig = webSocketKafkaProxyConfig;
        this.gson = gson;
    }

    public CompletableFuture<Response> register(Long userId, PluginReqisterQuery pluginReqisterQuery, PluginUpdate pluginUpdate,
                                                String authorization) {
        PluginVO existingPlugin = pluginService.findByName(pluginUpdate.getName());
        if (existingPlugin != null) {
            logger.error("Plugin with name {} already exists", pluginUpdate.getName());
            throw new HiveException(String.format(Messages.PLUGIN_ALREADY_EXISTS, pluginUpdate.getName()), BAD_REQUEST.getStatusCode());
        }

        return persistPlugin(pluginUpdate, pluginReqisterQuery.constructFilterString(), userId).thenApply(pluginVO -> {
            JwtTokenVO jwtTokenVO = createPluginTokens(pluginVO.getTopicName(), authorization);
            JsonObject response = createTokenResponse(pluginVO.getTopicName(), jwtTokenVO);

            return ResponseFactory.response(CREATED, response, PLUGIN_SUBMITTED);
        });
    }

    @Transactional
    public CompletableFuture<Response> update(PluginVO existingPlugin, PluginUpdateQuery pluginUpdateQuery, String authorization) {
        return updatePlugin(existingPlugin, pluginUpdateQuery).thenApply(plugin ->
            ResponseFactory.response(NO_CONTENT)
        );
    }

    @Transactional
    public CompletableFuture<Response> delete(PluginVO existingPlugin, String authorization) {
        pluginService.delete(existingPlugin.getId());

        CompletableFuture<com.devicehive.shim.api.Response> future = new CompletableFuture<>();
        if (existingPlugin.getSubscriptionId() != null) {
            PluginUnsubscribeRequest request = new PluginUnsubscribeRequest(existingPlugin.getSubscriptionId(), existingPlugin.getTopicName());
            rpcClient.call(Request.newBuilder()
                    .withBody(request)
                    .build(), new ResponseConsumer(future));
        }

        return future.thenApply(response -> ResponseFactory.response(NO_CONTENT));
    }

    public CompletableFuture<List<PluginVO>> list(String name, String namePattern, String topicName, Integer status, Long userId,
                                                  String sortField, String sortOrderSt, Integer take, Integer skip,
                                                  HivePrincipal principal) {

        ListPluginRequest listPluginRequest = new ListPluginRequest(name, namePattern, topicName, status, userId,
                sortField, sortOrderSt, take, skip, principal);

        return list(listPluginRequest);
    }

    public CompletableFuture<List<PluginVO>> list(ListPluginRequest listPluginRequest){
        CompletableFuture<com.devicehive.shim.api.Response> future = new CompletableFuture<>();

        rpcClient.call(Request
                .newBuilder()
                .withBody(listPluginRequest)
                .build(), new ResponseConsumer(future));

        return future.thenApply(response -> ((ListPluginResponse) response.getBody()).getPlugins());
    }

    public CompletableFuture<EntityCountResponse> count(String name, String namePattern, String topicName,
                                                        Integer status, Long userId, HivePrincipal principal) {
        CountPluginRequest countPluginRequest = new CountPluginRequest(name, namePattern, topicName, status, userId, principal);

        return count(countPluginRequest);
    }

    public CompletableFuture<EntityCountResponse> count(CountPluginRequest countPluginRequest) {
        CompletableFuture<com.devicehive.shim.api.Response> future = new CompletableFuture<>();

        rpcClient.call(Request
                .newBuilder()
                .withBody(countPluginRequest)
                .build(), new ResponseConsumer(future));

        return future.thenApply(response -> new EntityCountResponse((CountResponse) response.getBody()));
    }

    private CompletableFuture<PluginVO> persistPlugin(PluginUpdate pluginUpdate, String filterString, Long userId) {
        hiveValidator.validate(pluginUpdate);
        PluginVO pluginVO = pluginUpdate.convertTo();
        pluginVO.setUserId(userId);
        pluginVO.setFilter(filterString);
        pluginVO.setStatus(PluginStatus.CREATED);

        //Creation of topic for plugin
        String pluginTopic = "plugin_topic_" + UUID.randomUUID().toString();
        kafkaTopicService.createTopic(pluginTopic);
        pluginVO.setTopicName(pluginTopic);

        pluginService.create(pluginVO);

        return CompletableFuture.completedFuture(pluginVO);
    }

    private CompletableFuture<PluginVO> updatePlugin(PluginVO existingPlugin, PluginUpdateQuery pluginUpdateQuery) {
        if (pluginUpdateQuery.getStatus()!= null && pluginUpdateQuery.getStatus().equals(PluginStatus.CREATED)) {
            throw new IllegalArgumentException("Cannot change status of existing plugin to Created.");
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

        CompletableFuture<com.devicehive.shim.api.Response> future = new CompletableFuture<>();

        BasePluginRequest request = null;
        if (existingPlugin.getStatus().equals(PluginStatus.ACTIVE) && existingPlugin.getSubscriptionId() == null) {
            final Long subscriptionId = idGenerator.generate();
            request = pluginUpdateQuery.toRequest(filterService);
            request.setSubscriptionId(subscriptionId);
            existingPlugin.setSubscriptionId(subscriptionId);
            ((PluginSubscribeRequest) request).setTopicName(existingPlugin.getTopicName());
        }

        if (existingPlugin.getStatus().equals(PluginStatus.INACTIVE) && existingPlugin.getSubscriptionId() != null) {
            request = new PluginUnsubscribeRequest(existingPlugin.getSubscriptionId(), existingPlugin.getTopicName());
            existingPlugin.setSubscriptionId(null);
        }

        pluginService.update(existingPlugin);

        if (request != null) {
            rpcClient.call(Request.newBuilder()
                    .withBody(request)
                    .build(), new ResponseConsumer(future));
        }

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
        response.addProperty("proxyEndpoint", webSocketKafkaProxyConfig.getProxyPluginConnect());
        response.addProperty("topicName", topicName);

        return response;
    }

}

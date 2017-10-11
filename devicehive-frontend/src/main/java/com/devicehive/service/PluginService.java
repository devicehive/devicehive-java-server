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


import com.devicehive.dao.PluginDao;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.rpc.PluginSubscribeRequest;
import com.devicehive.model.updates.PluginUpdate;
import com.devicehive.service.helpers.LongIdGenerator;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.kafka.topic.KafkaTopicService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.PluginVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class PluginService {
    private static final Logger logger = LoggerFactory.getLogger(PluginService.class);
    
    private final HiveValidator hiveValidator;
    private final PluginDao pluginDao;
    private final KafkaTopicService kafkaTopicService;
    private final RpcClient rpcClient;
    private final LongIdGenerator idGenerator;
    private final DeviceService deviceService;

    @Autowired
    public PluginService(
            HiveValidator hiveValidator,
            PluginDao pluginDao,
            KafkaTopicService kafkaTopicService,
            RpcClient rpcClient,
            LongIdGenerator idGenerator,
            DeviceService deviceService) {
        this.hiveValidator = hiveValidator;
        this.pluginDao = pluginDao;
        this.kafkaTopicService = kafkaTopicService;
        this.rpcClient = rpcClient;
        this.idGenerator = idGenerator;
        this.deviceService = deviceService;
    }

    @Transactional
    public CompletableFuture<PluginVO> register(PluginSubscribeRequest pollRequest, PluginUpdate pluginUpdate) {
        hiveValidator.validate(pluginUpdate);
        PluginVO pluginVO = pluginUpdate.convertTo();
        if (pluginVO.getHealthCheckPeriod() == null) {
            pluginVO.setHealthCheckPeriod(300);
        }
        
        //Creation of topic for plugin
        String pluginTopic = "plugin_topic_" + UUID.randomUUID().toString();
        kafkaTopicService.createTopic(pluginTopic);
        pluginVO.setTopicName(pluginTopic);
        pluginDao.persist(pluginVO);
        
        
        //Creation of subscription for plugin
        final Long subscriptionId = idGenerator.generate();
        pollRequest.setSubscriptionId(subscriptionId);
        pollRequest.setTopicName(pluginTopic);
        
        //Update deviceIds in Filter taking into account networkIds and permissions
        Filter filter = pollRequest.getFilter();
        filter.setDeviceIds(deviceService.getAvailableDeviceIds(filter.getDeviceIds(), filter.getNetworkIds()));
        
        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(pollRequest)
                .build(), new ResponseConsumer(future));
        return future.thenApply(r -> pluginVO);
    }
}

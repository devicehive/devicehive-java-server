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
import com.devicehive.model.rpc.PluginUnsubscribeRequest;
import com.devicehive.proxy.PluginProxyClient;
import com.devicehive.service.helpers.HttpRestHelper;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.vo.PluginVO;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.ServiceUnavailableException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.devicehive.model.enums.PluginStatus.ACTIVE;
import static com.devicehive.model.enums.PluginStatus.DISABLED;

@Component
public class PluginHealthCheckService {
    private static final Logger logger = LoggerFactory.getLogger(PluginHealthCheckService.class);

    private final PluginService pluginService;
    private final PluginProxyClient rpcClient;
    private final HttpRestHelper httpRestHelper;

    @Autowired
    public PluginHealthCheckService(
            PluginService pluginService,
            PluginProxyClient rpcClient,
            HttpRestHelper httpRestHelper) {
        this.pluginService = pluginService;
        this.rpcClient = rpcClient;
        this.httpRestHelper = httpRestHelper;
    }

    @Scheduled(fixedDelayString = "${health.check.period}", initialDelayString = "${health.initial.delay}")
    @Transactional
    public void performHealthCheck() {
        List<PluginVO> plugins = pluginService.findByStatus(ACTIVE);
        plugins.forEach(pluginVO -> {
            try {
                httpRestHelper.get(pluginVO.getHealthCheckUrl(), JsonObject.class, null);
            } catch (ServiceUnavailableException e) {
                logger.warn("Plugin Service is Unavailable. Plugin " + pluginVO.getName() + "will be disabled", e);
                disablePlugin(pluginVO);
            }
        });
    }

    private CompletableFuture<PluginVO> disablePlugin(PluginVO pluginVO) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(new PluginUnsubscribeRequest(pluginVO.getSubscriptionId(), pluginVO.getTopicName()))
                .build(), new ResponseConsumer(future));

        pluginVO.setStatus(DISABLED);
        pluginService.update(pluginVO);

        return future.thenApply(response -> pluginVO);
    }
}

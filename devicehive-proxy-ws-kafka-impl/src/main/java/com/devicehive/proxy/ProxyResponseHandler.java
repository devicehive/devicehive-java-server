package com.devicehive.proxy;

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

import com.devicehive.api.RequestResponseMatcher;
import com.devicehive.model.ServerEvent;
import com.devicehive.proxy.api.NotificationHandler;
import com.devicehive.proxy.api.ProxyClient;
import com.devicehive.proxy.api.ProxyMessageBuilder;
import com.devicehive.proxy.api.payload.NotificationCreatePayload;
import com.devicehive.proxy.client.WebSocketKafkaProxyClient;
import com.devicehive.proxy.config.WebSocketKafkaProxyConfig;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.google.gson.Gson;
import com.lmax.disruptor.WorkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ProxyResponseHandler implements NotificationHandler, WorkHandler<ServerEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ProxyResponseHandler.class);

    private final Gson gson;
    private final String requestTopic;
    private final String replyToTopic;
    private final ProxyClient proxyClient;
    private final RequestResponseMatcher requestResponseMatcher;

    @Autowired
    public ProxyResponseHandler(Gson gson, String requestTopic, String replyToTopic, WebSocketKafkaProxyConfig proxyConfig, RequestResponseMatcher requestResponseMatcher) {
        this.gson = gson;
        this.requestTopic = requestTopic;
        this.replyToTopic = replyToTopic;
        this.requestResponseMatcher = requestResponseMatcher;
        WebSocketKafkaProxyClient webSocketKafkaProxyClient = new WebSocketKafkaProxyClient((message, client) -> {});
        webSocketKafkaProxyClient.setWebSocketKafkaProxyConfig(proxyConfig);
        this.proxyClient = webSocketKafkaProxyClient;
    }

    public void start() {
        proxyClient.start();
    }

    @Override
    public void handle(String message, ProxyClient client) {
        logger.debug("Received message from proxy client: " + message);
        final Response response = gson.fromJson(message, Response.class);

        requestResponseMatcher.offerResponse(response);
    }

    @Override
    public void onEvent(ServerEvent serverEvent) throws Exception {
        final Request request = serverEvent.get();
        if (request.getBody() == null) {
            throw new NullPointerException("Request body must not be null.");
        }
        request.setReplyTo(replyToTopic);

        proxyClient.push(ProxyMessageBuilder.notification(
                new NotificationCreatePayload(requestTopic, gson.toJson(request), request.getPartitionKey())));
    }
}

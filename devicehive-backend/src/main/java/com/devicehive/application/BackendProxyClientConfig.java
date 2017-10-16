package com.devicehive.application;

/*
 * #%L
 * DeviceHive Backend Logic
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

import com.devicehive.proxy.ProxyMessageDispatcher;
import com.devicehive.proxy.ProxyRequestHandler;
import com.devicehive.proxy.api.NotificationHandler;
import com.devicehive.proxy.api.ProxyClient;
import com.devicehive.proxy.api.ProxyMessageBuilder;
import com.devicehive.proxy.api.payload.TopicCreatePayload;
import com.devicehive.proxy.api.payload.TopicSubscribePayload;
import com.devicehive.proxy.client.WebSocketKafkaProxyClient;
import com.devicehive.proxy.config.WebSocketKafkaProxyConfig;
import com.devicehive.shim.api.server.MessageDispatcher;
import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.devicehive.configuration.Constants.REQUEST_TOPIC;

@Configuration
@Profile("ws-kafka-proxy")
@ComponentScan({"com.devicehive.proxy.config", "com.devicehive.proxy.client"})
public class BackendProxyClientConfig {

    @Bean
    public NotificationHandler notificationHandler(Gson gson, RequestHandlersMapper requestHandlersMapper) {
        return new ProxyRequestHandler(gson, requestHandlersMapper);
    }

    @Bean
    public ProxyClient proxyClient(NotificationHandler notificationHandler, WebSocketKafkaProxyConfig proxyConfig) {
        WebSocketKafkaProxyClient client = new WebSocketKafkaProxyClient(notificationHandler);
        client.setWebSocketKafkaProxyConfig(proxyConfig);
        client.start();
        client.push(ProxyMessageBuilder.create(new TopicCreatePayload(REQUEST_TOPIC)));
        client.push(ProxyMessageBuilder.subscribe(new TopicSubscribePayload(REQUEST_TOPIC))); // toDo: consumerGroup???
        return client;
    }

    @Bean
    public MessageDispatcher messageDispatcher(Gson gson, WebSocketKafkaProxyConfig proxyConfig) {
        return new ProxyMessageDispatcher(gson, proxyConfig);
    }
}

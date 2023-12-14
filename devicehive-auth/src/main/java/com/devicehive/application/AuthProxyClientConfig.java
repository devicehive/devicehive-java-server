package com.devicehive.application;

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

import com.devicehive.api.RequestResponseMatcher;
import com.devicehive.proxy.AuthRpcClient;
import com.devicehive.proxy.ProxyResponseHandler;
import com.devicehive.proxy.api.NotificationHandler;
import com.devicehive.proxy.client.WebSocketKafkaProxyClient;
import com.devicehive.proxy.config.WebSocketKafkaProxyConfig;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

import static com.devicehive.configuration.Constants.REQUEST_TOPIC;

@Configuration
@Profile({"ws-kafka-proxy"})
@ComponentScan({"com.devicehive.proxy.config", "com.devicehive.proxy.client"})
public class AuthProxyClientConfig {

    private final String responseTopic;

    public AuthProxyClientConfig(@Value("${response.topic.perfix}") final String responseTopicPrefix) {
        this.responseTopic = responseTopicPrefix + UUID.randomUUID();
    }

    @Bean
    public RequestResponseMatcher requestResponseMatcher() {
        return new RequestResponseMatcher();
    }

    @Bean
    public NotificationHandler notificationHandler(Gson gson, RequestResponseMatcher requestResponseMatcher, WebSocketKafkaProxyConfig proxyConfig) {
        return new ProxyResponseHandler(gson, REQUEST_TOPIC, responseTopic, proxyConfig, requestResponseMatcher);
    }

    @Bean
    public AuthRpcClient rpcClient(NotificationHandler notificationHandler, WebSocketKafkaProxyConfig proxyConfig, RequestResponseMatcher requestResponseMatcher, Gson gson) {
        WebSocketKafkaProxyClient proxyClient = new WebSocketKafkaProxyClient(notificationHandler);
        proxyClient.setWebSocketKafkaProxyConfig(proxyConfig);
        AuthRpcClient client = new AuthRpcClient(REQUEST_TOPIC, responseTopic, proxyClient, requestResponseMatcher, gson);
        client.start();
        return client;
    }
}

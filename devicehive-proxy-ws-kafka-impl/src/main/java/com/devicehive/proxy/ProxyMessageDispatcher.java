package com.devicehive.proxy;

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

import com.devicehive.proxy.api.ProxyClient;
import com.devicehive.proxy.api.ProxyMessage;
import com.devicehive.proxy.api.ProxyMessageBuilder;
import com.devicehive.proxy.api.payload.NotificationCreatePayload;
import com.devicehive.proxy.client.WebSocketKafkaProxyClient;
import com.devicehive.proxy.config.WebSocketKafkaProxyConfig;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.MessageDispatcher;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;

public class ProxyMessageDispatcher implements MessageDispatcher {

    private final Gson gson;
    private final ProxyClient proxyClient;

    @Autowired
    public ProxyMessageDispatcher(Gson gson, WebSocketKafkaProxyConfig proxyConfig) {
        this.gson = gson;
        WebSocketKafkaProxyClient webSocketKafkaProxyClient = new WebSocketKafkaProxyClient((message, client) -> {});
        webSocketKafkaProxyClient.setWebSocketKafkaProxyConfig(proxyConfig);
        this.proxyClient = webSocketKafkaProxyClient;
        this.proxyClient.start();
    }

    @Override
    public void send(String to, Response response) {
        ProxyMessage responseMessage = ProxyMessageBuilder.notification(new NotificationCreatePayload(to, gson.toJson(response)));
        proxyClient.push(responseMessage);
    }
}

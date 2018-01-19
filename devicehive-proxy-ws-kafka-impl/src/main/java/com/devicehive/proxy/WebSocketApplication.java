package com.devicehive.proxy;

/*
 * #%L
 * DeviceHive Proxy WebSocket Kafka Implementation
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

import com.devicehive.proxy.api.ProxyMessage;
import com.devicehive.proxy.api.ProxyMessageBuilder;
import com.devicehive.proxy.api.ProxyClient;
import com.devicehive.proxy.api.payload.SubscribePayload;
import com.devicehive.proxy.api.payload.TopicsPayload;
import com.devicehive.proxy.client.WebSocketKafkaProxyClient;

import java.util.concurrent.CompletableFuture;

public class WebSocketApplication {

    public static void main(String[] args) {

        ProxyClient client = new WebSocketKafkaProxyClient((message, proxyClient) -> System.out.println("Received message: " + message));
        client.start();

        CompletableFuture<ProxyMessage> healthFuture = client.push(ProxyMessageBuilder.health());
        System.out.println("Topic health: " + healthFuture.join());

        CompletableFuture<ProxyMessage> subscribeFuture = client.push(ProxyMessageBuilder.subscribe(new SubscribePayload("kafka-ws-topic")));
        System.out.println("Topic subscribe: " + subscribeFuture.join());

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }
}

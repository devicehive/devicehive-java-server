package com.devicehive.proxy.eventbus;

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

import com.devicehive.exceptions.HiveException;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.eventbus.FilterRegistry;
import com.devicehive.model.eventbus.SubscribeMessage;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.proxy.api.ProxyMessageBuilder;
import com.devicehive.proxy.api.payload.MessagePayload;
import com.devicehive.proxy.api.payload.NotificationCreatePayload;
import com.devicehive.proxy.api.payload.SubscribePayload;
import com.devicehive.proxy.client.WebSocketKafkaProxyClient;
import com.devicehive.proxy.config.WebSocketKafkaProxyConfig;
import com.google.gson.Gson;

import java.util.*;

import static com.devicehive.configuration.Constants.SUBSCRIPTION_TOPIC;
import static com.devicehive.model.eventbus.SubscribeAction.REGISTER;
import static com.devicehive.model.eventbus.SubscribeAction.UNREGISTER;

public class DistributedProxyFilterRegistry extends FilterRegistry {

    private final Gson gson;

    private final WebSocketKafkaProxyClient proxyClient; // todo: fault tolerance, synchronisation, lifetime of kafka topic (log.retention)

    public DistributedProxyFilterRegistry(Gson gson, WebSocketKafkaProxyConfig proxyConfig) {
        this.gson = gson;
        this.proxyClient = new WebSocketKafkaProxyClient((message, proxyClient) -> handleSubscriptionMessage(message, gson));
        proxyClient.setWebSocketKafkaProxyConfig(proxyConfig);
        proxyClient.start();
        proxyClient.push(ProxyMessageBuilder.subscribe(new SubscribePayload(SUBSCRIPTION_TOPIC, "fr-" + UUID.randomUUID()))).thenAccept(message -> {
            if (message.getStatus() == null || message.getStatus() != 0) {
                MessagePayload payload = (MessagePayload) message.getPayload();
                throw new HiveException("Response message is failed: " + payload.getMessage());
            }
        });
    }

    @Override
    public void register(Filter filter, Subscriber subscriber) {
        processRegister(filter, subscriber);

        String subscribeMessage = gson.toJson(new SubscribeMessage(REGISTER, filter, subscriber));
        proxyClient.push(ProxyMessageBuilder.notification(
                new NotificationCreatePayload(SUBSCRIPTION_TOPIC, subscribeMessage))).thenAccept(message -> {
            if (message.getStatus() == null || message.getStatus() != 0) {
                MessagePayload payload = (MessagePayload) message.getPayload();
                throw new HiveException("Response message is failed: " + payload.getMessage());
            }
        });
    }

    @Override
    public void unregister(Subscriber subscriber) {
        processUnregister(subscriber);

        String subscribeMessage = gson.toJson(new SubscribeMessage(UNREGISTER, subscriber));
        proxyClient.push(ProxyMessageBuilder.notification(
                new NotificationCreatePayload(SUBSCRIPTION_TOPIC, subscribeMessage))).thenAccept(message -> {
            if (message.getStatus() == null || message.getStatus() != 0) {
                MessagePayload payload = (MessagePayload) message.getPayload();
                throw new HiveException("Response message is failed: " + payload.getMessage());
            }
        });
    }
}

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
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.proxy.api.ProxyClient;
import com.devicehive.proxy.api.ProxyMessageBuilder;
import com.devicehive.proxy.api.payload.MessagePayload;
import com.devicehive.proxy.api.payload.NotificationCreatePayload;
import com.devicehive.proxy.api.payload.TopicsPayload;
import com.devicehive.proxy.client.WebSocketKafkaProxyClient;
import com.devicehive.proxy.config.WebSocketKafkaProxyConfig;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.Gson;

import java.util.*;

import static com.devicehive.configuration.Constants.SUBSCRIPTION_TOPIC;
import static com.devicehive.proxy.eventbus.SubscribeAction.REGISTER;
import static com.devicehive.proxy.eventbus.SubscribeAction.UNREGISTER;

public class DistributedFilterRegistry extends FilterRegistry {

    private final Gson gson;

    private final WebSocketKafkaProxyClient proxyClient; // todo: fault tolerance, synchronisation, lifetime of kafka topic (log.retention)

    public DistributedFilterRegistry(Gson gson, WebSocketKafkaProxyConfig proxyConfig) {
        this.gson = gson;
        this.proxyClient = new WebSocketKafkaProxyClient((message, proxyClient) -> {
            SubscribeMessage subscribeMessage = gson.fromJson(message, SubscribeMessage.class);

            switch (subscribeMessage.getAction()) {
                case REGISTER:
                    super.register(subscribeMessage.getFilter(), subscribeMessage.getSubscriber());
                    break;
                case UNREGISTER:
                    super.unregister(subscribeMessage.getSubscriber());
                    break;
            }
        });
        proxyClient.setWebSocketKafkaProxyConfig(proxyConfig);
        proxyClient.start();
        proxyClient.push(ProxyMessageBuilder.subscribe(new TopicsPayload(SUBSCRIPTION_TOPIC))).thenAccept(message -> {
            if (message.getStatus() == null || message.getStatus() != 0) {
                MessagePayload payload = (MessagePayload) message.getPayload();
                throw new HiveException("Response message is failed: " + payload.getMessage());
            }
        });
    }

    @Override
    public void register(Filter filter, Subscriber subscriber) {
        super.register(filter, subscriber);

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
        super.unregister(subscriber);

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

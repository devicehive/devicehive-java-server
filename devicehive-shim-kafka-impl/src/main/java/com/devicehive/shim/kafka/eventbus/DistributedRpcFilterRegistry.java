package com.devicehive.shim.kafka.eventbus;

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

import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.eventbus.FilterRegistry;
import com.devicehive.model.eventbus.SubscriptionSyncMessage;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.shim.config.KafkaRpcConfig;
import com.devicehive.shim.kafka.KafkaMessageHandler;
import com.devicehive.vo.DeviceVO;
import com.google.gson.Gson;

import java.util.Collection;

import static com.devicehive.configuration.Constants.SUBSCRIPTION_TOPIC;
import static com.devicehive.model.eventbus.SubscribeAction.*;

public class DistributedRpcFilterRegistry extends FilterRegistry {

    private final Gson gson;

    private final KafkaMessageHandler messageHandler; // todo: fault tolerance, synchronisation, lifetime of kafka topic (log.retention)

    public DistributedRpcFilterRegistry(Gson gson, KafkaRpcConfig kafkaRpcConfig) {
        this.gson = gson;
        this.messageHandler = new KafkaMessageHandler(kafkaRpcConfig, SUBSCRIPTION_TOPIC) {
            @Override
            public void handle(String message) {
                handleSubscriptionMessage(message, gson);
            }
        };
        messageHandler.start();
    }

    @Override
    public void register(Filter filter, Subscriber subscriber) {
        processRegister(filter, subscriber);

        String syncMessage = gson.toJson(new SubscriptionSyncMessage(REGISTER, filter, subscriber));
        messageHandler.push(syncMessage);
    }

    @Override
    public void unregister(Subscriber subscriber) {
        processUnregister(subscriber);

        String syncMessage = gson.toJson(new SubscriptionSyncMessage(UNREGISTER, subscriber));
        messageHandler.push(syncMessage);
    }

    @Override
    public void unregisterDevice(DeviceVO device) {
        processUnregisterDevice(device);

        String syncMessage = gson.toJson(new SubscriptionSyncMessage(UNREGISTER_DEVICE, device));
        messageHandler.push(syncMessage);
    }

    @Override
    public void unregisterNetwork(Long networkId, Collection<DeviceVO> devices) {
        processUnregisterNetwork(networkId, devices);

        String syncMessage = gson.toJson(new SubscriptionSyncMessage(UNREGISTER_NETWORK, devices, networkId, null));
        messageHandler.push(syncMessage);
    }

    @Override
    public void unregisterDeviceType(Long deviceTypeId, Collection<DeviceVO> devices) {
        processUnregisterDeviceType(deviceTypeId, devices);

        String syncMessage = gson.toJson(new SubscriptionSyncMessage(UNREGISTER_DEVICE_TYPE, devices, null, deviceTypeId));
        messageHandler.push(syncMessage);
    }
}

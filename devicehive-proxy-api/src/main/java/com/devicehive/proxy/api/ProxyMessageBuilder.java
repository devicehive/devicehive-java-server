package com.devicehive.proxy.api;

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

import com.devicehive.proxy.api.payload.SubscribePayload;
import com.devicehive.proxy.api.payload.TopicsPayload;
import com.devicehive.proxy.api.payload.NotificationCreatePayload;

public class ProxyMessageBuilder {

    public static ProxyMessage create(TopicsPayload payload) {
        return ProxyMessage.newBuilder()
                .withType("topic")
                .withAction("create")
                .withPayload(payload)
                .build();
    }

    public static ProxyMessage list() {
        return ProxyMessage.newBuilder()
                .withType("topic")
                .withAction("list")
                .build();
    }

    public static ProxyMessage subscribe(SubscribePayload payload) {
        return ProxyMessage.newBuilder()
                .withType("topic")
                .withAction("subscribe")
                .withPayload(payload)
                .build();
    }

    public static ProxyMessage unsubscribe() {
        return ProxyMessage.newBuilder()
                .withType("topic")
                .withAction("unsubscribe")
                .build();
    }

    public static ProxyMessage notification(NotificationCreatePayload payload) {
        return ProxyMessage.newBuilder()
                .withType("notif")
                .withAction("create")
                .withPayload(payload)
                .build();
    }

    public static ProxyMessage health() {
        return ProxyMessage.newBuilder()
                .withType("health")
                .build();
    }
}

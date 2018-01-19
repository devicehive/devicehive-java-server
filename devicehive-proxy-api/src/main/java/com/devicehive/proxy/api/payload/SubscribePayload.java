package com.devicehive.proxy.api.payload;

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

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class SubscribePayload implements Payload {

    @SerializedName("t")
    private List<String> topics;

    @SerializedName("sg")
    private String subscriptionGroup;

    public SubscribePayload(List<String> topics) {
        this.topics = topics;
    }

    public SubscribePayload(String topic) {
        this.topics = Collections.singletonList(topic);
    }

    public SubscribePayload(List<String> topics, String subscriptionGroup) {
        this.topics = topics;
        this.subscriptionGroup = subscriptionGroup;
    }

    public SubscribePayload(String topic, String subscriptionGroup) {
        this.topics = Collections.singletonList(topic);
        this.subscriptionGroup = subscriptionGroup;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public String getSubscriptionGroup() {
        return subscriptionGroup;
    }

    public void setSubscriptionGroup(String subscriptionGroup) {
        this.subscriptionGroup = subscriptionGroup;
    }

    @Override
    public String toString() {
        return "TopicsPayload{" +
                "topics=" + topics +
                ", subscriptionGroup=" + subscriptionGroup +
                '}';
    }
}

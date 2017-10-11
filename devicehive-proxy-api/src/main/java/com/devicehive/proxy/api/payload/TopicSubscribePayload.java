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

public class TopicSubscribePayload implements Payload {


    @SerializedName("t")
    private List<String> topics;

    @SerializedName("consumer_group")
    private String consumerGroup;

    public TopicSubscribePayload(List<String> topics, String consumerGroup) {
        this.topics = topics;
        this.consumerGroup = consumerGroup;
    }

    public TopicSubscribePayload(List<String> topics) {
        this.topics = topics;
    }

    public TopicSubscribePayload(String topic) {
        this.topics = Collections.singletonList(topic);
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    @Override
    public String toString() {
        return "TopicSubscribePayload{" +
                "topics=" + topics +
                ", consumerGroup='" + consumerGroup + '\'' +
                '}';
    }
}

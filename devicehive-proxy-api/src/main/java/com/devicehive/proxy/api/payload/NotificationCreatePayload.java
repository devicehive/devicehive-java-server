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

public class NotificationCreatePayload implements Payload {

    @SerializedName("t")
    private String topic;

    @SerializedName("m")
    private String message;

    @SerializedName("part")
    private String partition;

    public NotificationCreatePayload(String topic, String message) {
        this.topic = topic;
        this.message = message;
        this.partition = "0";
    }

    public NotificationCreatePayload(String topic, String message, String partition) {
        this.topic = topic;
        this.message = message;
        this.partition = partition;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }

    @Override
    public String toString() {
        return "NotificationCreatePayload{" +
                "topic='" + topic + '\'' +
                ", message='" + message + '\'' +
                ", partition='" + partition + '\'' +
                '}';
    }
}

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

public class HealthPayload implements Payload {

    @SerializedName("prx")
    private String proxyStatus;

    @SerializedName("mb")
    private String messageBufferStatus;

    @SerializedName("mbfp")
    private Double messageBufferFillPercentage;

    @SerializedName("comm")
    private String messageBrokerStatus;

    public HealthPayload(String proxyStatus, String messageBufferStatus, Double messageBufferFillPercentage, String messageBrokerStatus) {
        this.proxyStatus = proxyStatus;
        this.messageBufferStatus = messageBufferStatus;
        this.messageBufferFillPercentage = messageBufferFillPercentage;
        this.messageBrokerStatus = messageBrokerStatus;
    }

    public String getProxyStatus() {
        return proxyStatus;
    }

    public void setProxyStatus(String proxyStatus) {
        this.proxyStatus = proxyStatus;
    }

    public String getMessageBufferStatus() {
        return messageBufferStatus;
    }

    public void setMessageBufferStatus(String messageBufferStatus) {
        this.messageBufferStatus = messageBufferStatus;
    }

    public Double getMessageBufferFillPercentage() {
        return messageBufferFillPercentage;
    }

    public void setMessageBufferFillPercentage(Double messageBufferFillPercentage) {
        this.messageBufferFillPercentage = messageBufferFillPercentage;
    }

    public String getMessageBrokerStatus() {
        return messageBrokerStatus;
    }

    public void setMessageBrokerStatus(String messageBrokerStatus) {
        this.messageBrokerStatus = messageBrokerStatus;
    }

    @Override
    public String toString() {
        return "HealthPayload{" +
                "proxyStatus='" + proxyStatus + '\'' +
                ", messageBufferStatus='" + messageBufferStatus + '\'' +
                ", messageBufferFillPercentage=" + messageBufferFillPercentage +
                ", messageBrokerStatus='" + messageBrokerStatus + '\'' +
                '}';
    }
}

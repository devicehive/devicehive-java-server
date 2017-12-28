package com.devicehive.security.jwt;

/*
 * #%L
 * DeviceHive Common Module
 * %%
 * Copyright (C) 2016 DataArt
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.Set;

public class JwtPluginPayload extends JwtPayload {
    private static final long serialVersionUID = -501922541863466048L;

    public final static String TOPIC = "tpc";
    
    @SerializedName(TOPIC)
    @JsonProperty(TOPIC)
    private String topic;

    public JwtPluginPayload(Set<Integer> actions, String topic, Date expiration, Integer tokenType) {
        super(actions, expiration, tokenType);
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    @JsonIgnore
    public boolean isUserPayload() {
        return false;
    }

    public static JwtPluginPayloadBuilder newBuilder() {
        return new JwtPluginPayloadBuilder();
    }

    public static class JwtPluginPayloadBuilder extends JwtPayloadBuilder {
        private String topic;
        
        public JwtPluginPayloadBuilder withPublicClaims(String topic) {
            this.topic = topic;
            return this;
        }

        public JwtPluginPayloadBuilder withTopic(String topic) {
            this.topic = topic;
            return this;
        }

        public JwtPluginPayloadBuilder withPayload(JwtPluginPayload payload) {
            this.actions = payload.getActions();
            this.topic = payload.getTopic();
            this.withExpirationDate(payload.getExpiration());
            return this;
        }

        public JwtPluginPayload buildPayload() {
            return new JwtPluginPayload(actions, topic, expiration, tokenType);
        }
    }
}

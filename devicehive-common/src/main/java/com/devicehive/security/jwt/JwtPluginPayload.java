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

import com.devicehive.model.HiveEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class JwtPluginPayload implements HiveEntity {
    private static final long serialVersionUID = -501922541863466048L;

    private final static String TOPICS = "tpc";
    public final static String EXPIRATION = "exp";
    public final static String TOKEN_TYPE = "ttp";

    @SerializedName(TOPICS)
    @JsonProperty(TOPICS)
    private String topic;

    @SerializedName(EXPIRATION)
    @JsonProperty(EXPIRATION)
    private Date expiration;

    @SerializedName(TOKEN_TYPE)
    @ApiModelProperty(hidden = true)
    private Integer tokenType;

    public JwtPluginPayload(String topic, Date expiration, Integer tokenType) {
        this.topic = topic;
        this.expiration = expiration;
        this.tokenType = tokenType;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Date getExpiration() {
        return expiration;
    }
    
    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    @JsonProperty(TOKEN_TYPE)
    public Integer getTokenType() {
        return tokenType;
    }

    public void setTokenType(Integer tokenType) {
        this.tokenType = tokenType;
    }

    public static JwtPluginPayloadBuilder newBuilder() {
        return new JwtPluginPayloadBuilder();
    }

    public static class JwtPluginPayloadBuilder {
        private String topic;
        private Date expiration;
        private Integer tokenType;

        public JwtPluginPayloadBuilder withPublicClaims(String topic) {
            this.topic = topic;
            return this;
        }

        public JwtPluginPayloadBuilder withPayload(JwtPluginPayload payload) {
            this.topic = payload.getTopic();
            this.expiration = payload.getExpiration();
            return this;
        }

        public JwtPluginPayloadBuilder withTokenType(Integer tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public JwtPluginPayloadBuilder withExpirationDate(Date expiration) {
            this.expiration = expiration;
            return this;
        }

        public JwtPluginPayload buildPayload() {
            return new JwtPluginPayload(topic, expiration, tokenType);
        }
    }
}

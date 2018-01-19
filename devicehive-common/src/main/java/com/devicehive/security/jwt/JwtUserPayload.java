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

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;

/**
 * Common JWT entity
 * Structure must be as provided below:
 * {
 *     "userId": user_id,
 *     "actions": ["action1","action2","actionN"],
 *     "networkIds": ["id1","id2","idN"],
 *     "deviceIds": ["id1","id2","idN"]
 * }
 *
 * To get admin permissions (to all actions, networks, etc) you have to specify "*" for string parameters:
 * {
 *     "userId": user_id,
 *     "actions": ["*"],
 *     "networkIds": ["*"],
 *     "deviceIds": ["*"]
 * }
 */
public class JwtUserPayload extends JwtPayload {

    private static final long serialVersionUID = -6904689203121394308L;

    public final static String USER_ID = "u";
    public final static String NETWORK_IDS = "n";
    public final static String DEVICE_TYPE_IDS = "dt";
    
    //Public claims

    @NotNull
    @SerializedName(USER_ID)
    @JsonProperty(USER_ID)
    private Long userId;

    @SerializedName(NETWORK_IDS)
    @JsonProperty(NETWORK_IDS)
    private Set<String> networkIds;

    @SerializedName(DEVICE_TYPE_IDS)
    @JsonProperty(DEVICE_TYPE_IDS)
    private Set<String> deviceTypeIds;

    public JwtUserPayload(Long userId, Set<Integer> actions, Set<String> networkIds,
                       Set<String> deviceTypeIds, Date expiration, Integer tokenType) {
        super(actions, expiration, tokenType);
        this.userId = userId;
        this.networkIds = networkIds;
        this.deviceTypeIds = deviceTypeIds;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Set<String> getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(Set<String> networkIds) {
        this.networkIds = networkIds;
    }

    public Set<String> getDeviceTypeIds() {
        return deviceTypeIds;
    }

    public void setDeviceTypeIds(Set<String> deviceTypeIds) {
        this.deviceTypeIds = deviceTypeIds;
    }

    @Override
    @JsonIgnore
    public boolean isUserPayload() {
        return true;
    }

    public static JwtUserPayloadBuilder newBuilder() {
        return new JwtUserPayloadBuilder();
    }

    public static class JwtUserPayloadBuilder extends JwtPayloadBuilder {
        private Long userId;
        private Set<String> networkIds;
        private Set<String> deviceTypeIds;
        
        public JwtUserPayloadBuilder withPublicClaims(Long userId, Set<Integer> actions,
                                        Set<String> networkIds, Set<String> deviceTypeIds) {
            this.userId = userId;
            this.actions = actions;
            this.networkIds = networkIds;
            this.deviceTypeIds = deviceTypeIds;
            return this;
        }

        public JwtUserPayloadBuilder withPayload(JwtUserPayload payload) {
            this.userId = payload.getUserId();
            this.actions = payload.getActions();
            this.networkIds = payload.getNetworkIds();
            this.deviceTypeIds = payload.getDeviceTypeIds();
            this.expiration = payload.getExpiration();
            return this;
        }

        public JwtUserPayloadBuilder withUserId(Long userId) {
            this.userId = userId;
            return this;
        }

        public JwtUserPayloadBuilder withActions(Set<Integer> actions) {
            this.actions = actions;
            return this;
        }

        public JwtUserPayloadBuilder withNetworkIds(Set<String> networkIds) {
            this.networkIds = networkIds;
            return this;
        }

        public JwtUserPayloadBuilder withDeviceTypeIds(Set<String> deviceTypeIds) {
            this.deviceTypeIds = deviceTypeIds;
            return this;
        }

        public JwtUserPayloadBuilder withTokenType(Integer tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public JwtUserPayloadBuilder withExpirationDate(Date expiration) {
            this.expiration = expiration;
            return this;
        }

        public JwtUserPayload buildPayload() {
            return new JwtUserPayload(userId, actions, networkIds, deviceTypeIds, expiration, tokenType);
        }
    }
}

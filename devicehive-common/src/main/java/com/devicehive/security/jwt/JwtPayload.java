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
import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;

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
 *     "deviceGuids": ["guid1","guid2","guidN"]
 * }
 *
 * To get admin permissions (to all actions, networks, etc) you have to specify "*" for string parameters:
 * {
 *     "userId": user_id,
 *     "actions": ["*"],
 *     "networkIds": ["*"],
 *     "deviceGuids": ["*"]
 * }
 */
public class JwtPayload implements HiveEntity {

    private static final long serialVersionUID = -6904689203121394308L;
    public static final String JWT_CLAIM_KEY = "payload";

    public final static String USER_ID = "userId";
    public final static String ACTIONS = "actions";
    public final static String NETWORK_IDS = "networkIds";
    public final static String DEVICE_GUIDS = "deviceGuids";
    public final static String EXPIRATION = "expiration";
    public final static String TOKEN_TYPE = "tokenType";

    //Public claims

    @NotNull
    @SerializedName("userId")
    private Long userId;

    @SerializedName("actions")
    private Set<String> actions;

    @SerializedName("networkIds")
    private Set<String> networkIds;

    @SerializedName("deviceGuids")
    private Set<String> deviceGuids;

    //Registered claims

    @SerializedName("expiration")
    private Date expiration;

    @SerializedName("tokenType")
    @ApiModelProperty(hidden = true)
    private TokenType tokenType;

    private JwtPayload(Long userId, Set<String> actions, Set<String> networkIds,
                       Set<String> deviceGuids, Date expiration, TokenType tokenType) {
        this.userId = userId;
        this.actions = actions;
        this.networkIds = networkIds;
        this.deviceGuids = deviceGuids;
        this.expiration = expiration;
        this.tokenType = tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Set<String> getActions() {
        return actions;
    }

    public void setActions(Set<String> actions) {
        this.actions = actions;
    }

    public Set<String> getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(Set<String> networkIds) {
        this.networkIds = networkIds;
    }

    public Set<String> getDeviceGuids() {
        return deviceGuids;
    }

    public void setDeviceGuids(Set<String> deviceGuids) {
        this.deviceGuids = deviceGuids;
    }

    public Date getExpiration() {
        return expiration;
    }
    
    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public TokenType getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private Set<String> actions;
        private Set<String> networkIds;
        private Set<String> deviceGuids;
        private Date expiration;
        private TokenType tokenType;

        public Builder withPublicClaims(Long userId, Set<String> actions,
                                        Set<String> networkIds, Set<String> deviceGuids) {
            this.userId = userId;
            this.actions = actions;
            this.networkIds = networkIds;
            this.deviceGuids = deviceGuids;
            return this;
        }

        public Builder withPayload(JwtPayload payload) {
            this.userId = payload.getUserId();
            this.actions = payload.getActions();
            this.networkIds = payload.getNetworkIds();
            this.deviceGuids = payload.getDeviceGuids();
            this.expiration = payload.getExpiration();
            return this;
        }

        public Builder withUserId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder withActions(Set<String> actions) {
            this.actions = actions;
            return this;
        }

        public Builder withNetworkIds(Set<String> networkIds) {
            this.networkIds = networkIds;
            return this;
        }

        public Builder withDeviceGuids(Set<String> deviceGuids) {
            this.deviceGuids = deviceGuids;
            return this;
        }

        public Builder withTokenType(TokenType tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Builder withExpirationDate(Date expiration) {
            this.expiration = expiration;
            return this;
        }

        public JwtPayload buildPayload() {
            return new JwtPayload(userId, actions, networkIds, deviceGuids, expiration, tokenType);
        }
    }
}

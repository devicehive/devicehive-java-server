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

import com.devicehive.auth.HiveAction;
import com.devicehive.model.HiveEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.devicehive.auth.HiveAction.NONE;

public class JwtPayloadView implements HiveEntity {

    private static final long serialVersionUID = 9015868660504625526L;

    private final static String USER_ID = "userId";
    private final static String ACTIONS = "actions";
    private final static String NETWORK_IDS = "networkIds";
    private final static String DEVICE_IDS = "deviceIds";
    private final static String EXPIRATION = "expiration";
    private final static String TOKEN_TYPE = "tokenType";

    @NotNull
    @JsonProperty(USER_ID)
    @SerializedName(USER_ID)
    private Long userId;

    @JsonProperty(ACTIONS)
    @SerializedName(ACTIONS)
    private Set<String> actions;

    @JsonProperty(NETWORK_IDS)
    @SerializedName(NETWORK_IDS)
    private Set<String> networkIds;

    @JsonProperty(DEVICE_IDS)
    @SerializedName(DEVICE_IDS)
    private Set<String> deviceIds;

    @JsonProperty(EXPIRATION)
    @SerializedName(EXPIRATION)
    private Date expiration;

    @ApiModelProperty(hidden = true)
    @SerializedName(TOKEN_TYPE)
    private TokenType tokenType;

    public JwtPayloadView(Long userId, Set<String> actions, Set<String> networkIds,
            Set<String> deviceIds, Date expiration, TokenType tokenType) {
        this.userId = userId;
        this.actions = actions;
        this.networkIds = networkIds;
        this.deviceIds = deviceIds;
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

    public Set<String> getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(Set<String> deviceIds) {
        this.deviceIds = deviceIds;
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

    public JwtPayload convertTo() {
        Set<Integer> actionIds = Optional.ofNullable(actions)
                .map(value -> value.stream()
                        //Here the compatibility with old behavior is provided to ignore not valid actions
                        .map(action -> HiveAction.fromString(action))
                        .filter(Objects::nonNull)
                        .mapToInt(HiveAction::getId)
                        .boxed()
                        .collect(Collectors.toSet()))
                .orElse(ImmutableSet.of(NONE.getId()));
        
        return new JwtPayload(userId, actionIds, networkIds, deviceIds, expiration, null);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private Set<String> actions;
        private Set<String> networkIds;
        private Set<String> deviceIds;
        private Date expiration;
        private TokenType tokenType;

        public Builder withPublicClaims(Long userId, Set<String> actions,
                Set<String> networkIds, Set<String> deviceIds) {
            this.userId = userId;
            this.actions = actions;
            this.networkIds = networkIds;
            this.deviceIds = deviceIds;
            return this;
        }

        public Builder withPayload(JwtPayloadView payload) {
            this.userId = payload.getUserId();
            this.actions = payload.getActions();
            this.networkIds = payload.getNetworkIds();
            this.deviceIds = payload.getDeviceIds();
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

        public Builder withDeviceIds(Set<String> deviceIds) {
            this.deviceIds = deviceIds;
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

        public JwtPayloadView buildPayload() {
            return new JwtPayloadView(userId, actions, networkIds, deviceIds, expiration, tokenType);
        }
    }
}

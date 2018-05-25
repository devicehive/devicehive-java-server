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
import javax.ws.rs.BadRequestException;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.devicehive.auth.HiveAction.NONE;

public class JwtUserPayloadView<T> implements HiveEntity {

    private static final long serialVersionUID = 9015868660504625526L;

    private final static String USER_ID = "userId";
    private final static String ACTIONS = "actions";
    private final static String NETWORK_IDS = "networkIds";
    private final static String DEVICE_TYPE_IDS = "deviceTypeIds";
    private final static String EXPIRATION = "expiration";
    private final static String REFRESH_EXPIRATION = "refreshExpiration";
    private final static String TOKEN_TYPE = "tokenType";

    @NotNull
    @JsonProperty(USER_ID)
    @SerializedName(USER_ID)
    private Long userId;

    @JsonProperty(ACTIONS)
    @SerializedName(ACTIONS)
    private Set<T> actions;

    @JsonProperty(NETWORK_IDS)
    @SerializedName(NETWORK_IDS)
    private Set<String> networkIds;

    @JsonProperty(DEVICE_TYPE_IDS)
    @SerializedName(DEVICE_TYPE_IDS)
    private Set<String> deviceTypeIds;

    @JsonProperty(EXPIRATION)
    @SerializedName(EXPIRATION)
    private Date expiration;

    @JsonProperty(REFRESH_EXPIRATION)
    @SerializedName(REFRESH_EXPIRATION)
    private Date refreshExpiration;

    @ApiModelProperty(hidden = true)
    @SerializedName(TOKEN_TYPE)
    private TokenType tokenType;

    public JwtUserPayloadView(Long userId, Set<T> actions, Set<String> networkIds,
            Set<String> deviceTypeIds, Date expiration, Date refreshExpiration, TokenType tokenType) {
        this.userId = userId;
        this.actions = actions;
        this.networkIds = networkIds;
        this.deviceTypeIds = deviceTypeIds;
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
        this.tokenType = tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Set<T> getActions() {
        return actions;
    }

    public void setActions(Set<T> actions) {
        this.actions = actions;
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

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public Date getRefreshExpiration() {
        return refreshExpiration;
    }

    public void setRefreshExpiration(Date refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public JwtUserPayload convertTo() {
        Set<Integer> actionIds = Optional.ofNullable(actions)
                .map(value -> value.stream()
                        //Here the compatibility with old behavior is provided to ignore not valid actions
                        .map(action -> {
                            if (action instanceof String) {
                                return HiveAction.fromString((String) action);
                            } else if (action instanceof Number && ((Double) action - ((Double) action).intValue() == 0)) {
                                return HiveAction.fromId(((Double) action).intValue());
                            } else throw new BadRequestException("Actions list should contain only Strings or Integers");
                        })
                        .filter(Objects::nonNull)
                        .mapToInt(HiveAction::getId)
                        .boxed()
                        .collect(Collectors.toSet()))
                .orElse(ImmutableSet.of(NONE.getId()));
        
        return new JwtUserPayload(userId, actionIds, networkIds, deviceTypeIds, expiration, null);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder<T> {
        private Long userId;
        private Set<T> actions;
        private Set<String> networkIds;
        private Set<String> deviceTypeIds;
        private Date expiration;
        private Date refreshExpiration;
        private TokenType tokenType;

        public Builder withPublicClaims(Long userId, Set<T> actions,
                                        Set<String> networkIds, Set<String> deviceTypeIds) {
            this.userId = userId;
            this.actions = actions;
            this.networkIds = networkIds;
            this.deviceTypeIds = deviceTypeIds;
            return this;
        }

        public Builder withPayload(JwtUserPayloadView<T> payload) {
            this.userId = payload.getUserId();
            this.actions = payload.getActions();
            this.networkIds = payload.getNetworkIds();
            this.deviceTypeIds = payload.getDeviceTypeIds();
            this.expiration = payload.getExpiration();
            this.refreshExpiration = payload.getRefreshExpiration();
            return this;
        }

        public Builder withUserId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder withActions(Set<T> actions) {
            this.actions = actions;
            return this;
        }

        public Builder withNetworkIds(Set<String> networkIds) {
            this.networkIds = networkIds;
            return this;
        }

        public Builder withDeviceTypeIds(Set<String> deviceTypeIds) {
            this.deviceTypeIds = deviceTypeIds;
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

        public Builder withRefreshExpirationDate(Date refreshExpiration) {
            this.refreshExpiration = refreshExpiration;
            return this;
        }

        public JwtUserPayloadView<T> buildPayload() {
            return new JwtUserPayloadView<T>(userId, actions, networkIds, deviceTypeIds, expiration, refreshExpiration, tokenType);
        }
    }
}

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
import java.util.Set;

public abstract class JwtPayload implements HiveEntity {
    private static final long serialVersionUID = -8814080198864472032L;

    public final static String JWT_CLAIM_KEY = "payload";
    public final static String ACTIONS = "a";
    public final static String EXPIRATION = "e";
    public final static String TOKEN_TYPE = "t";

    @SerializedName(ACTIONS)
    @JsonProperty(ACTIONS)
    private Set<Integer> actions;

    @SerializedName(EXPIRATION)
    @JsonProperty(EXPIRATION)
    private Date expiration;

    @SerializedName(TOKEN_TYPE)
    @ApiModelProperty(hidden = true)
    private Integer tokenType;

    public JwtPayload(Set<Integer> actions, Date expiration, Integer tokenType) {
        this.actions = actions;
        this.expiration = expiration;
        this.tokenType = tokenType;
    }

    public Set<Integer> getActions() {
        return actions;
    }

    public void setActions(Set<Integer> actions) {
        this.actions = actions;
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

    public abstract boolean isUserPayload();

    public abstract static class JwtPayloadBuilder {
        protected Date expiration;
        protected Integer tokenType;
        protected Set<Integer> actions;

        public JwtPayloadBuilder withActions(Set<Integer> actions) {
            this.actions = actions;
            return this;
        }

        public JwtPayloadBuilder withTokenType(Integer tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public JwtPayloadBuilder withExpirationDate(Date expiration) {
            this.expiration = expiration;
            return this;
        }

        public abstract JwtPayload buildPayload();
    }
}

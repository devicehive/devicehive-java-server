package com.devicehive.vo;

/*
 * #%L
 * DeviceHive Frontend Logic
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

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.JWT_ACCESS_TOKEN_SUBMITTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.JWT_REFRESH_TOKEN_SUBMITTED;

/**
 * JWT token response entity
 */
public class JwtTokenVO implements HiveEntity {

    private static final long serialVersionUID = 1934838110938833117L;

    @JsonPolicyDef({JWT_REFRESH_TOKEN_SUBMITTED, JWT_ACCESS_TOKEN_SUBMITTED})
    @SerializedName("accessToken")
    private String accessToken; 

    @JsonPolicyDef({JWT_REFRESH_TOKEN_SUBMITTED})
    @SerializedName("refreshToken")
    private String refreshToken;

    public JwtTokenVO() {
    }

    public String getAccessToken() {
        return accessToken;
    }
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

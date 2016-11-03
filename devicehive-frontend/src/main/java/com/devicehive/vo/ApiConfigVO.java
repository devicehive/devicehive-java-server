package com.devicehive.vo;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

import java.io.Serializable;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_SERVER_CONFIG;

/**
 * Created by tmatvienko on 12/2/14.
 */
public class ApiConfigVO implements HiveEntity {

    private static final long serialVersionUID = -4819848129715601667L;

    @JsonPolicyDef(REST_SERVER_CONFIG)
    @SerializedName("providers")
    private Set<IdentityProviderConfig> providerConfigs;

    @JsonPolicyDef(REST_SERVER_CONFIG)
    private Long sessionTimeout;

    public ApiConfigVO() {
    }

    public Set<IdentityProviderConfig> getProviderConfigs() {
        return providerConfigs;
    }

    public void setProviderConfigs(Set<IdentityProviderConfig> providerConfigs) {
        this.providerConfigs = providerConfigs;
    }

    public Long getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
}

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

import java.util.Objects;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_SERVER_CONFIG;

/**
 * Created by tmatvienko on 12/2/14.
 */
public class IdentityProviderConfig implements HiveEntity {

    private static final long serialVersionUID = -2274848199115698341L;

    @JsonPolicyDef(REST_SERVER_CONFIG)
    private String name;

    @JsonPolicyDef(REST_SERVER_CONFIG)
    private String clientId;

    public IdentityProviderConfig(String name) {
        this.name = name;
    }

    public IdentityProviderConfig(String name, String clientId) {
        this.name = name;
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentityProviderConfig)) return false;
        IdentityProviderConfig that = (IdentityProviderConfig) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, clientId);
    }

    @Override
    public String toString() {
        return "IdentityProviderConfig{" +
                "name='" + name + '\'' +
                ", clientId='" + clientId + '\'' +
                '}';
    }
}

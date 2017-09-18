package com.devicehive.model.eventbus;

/*
 * #%L
 * DeviceHive Common Module
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import com.devicehive.auth.HivePrincipal;
import com.devicehive.json.strategies.JsonPolicyDef;

import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.SUBSCRIPTIONS_LISTED;

public class Filter {

    private HivePrincipal principal;

    @JsonPolicyDef(SUBSCRIPTIONS_LISTED)
    private boolean global = false;

    @JsonPolicyDef(SUBSCRIPTIONS_LISTED)
    private Set<Long> networkIds;

    @JsonPolicyDef(SUBSCRIPTIONS_LISTED)
    private Set<String> deviceIds;

    @JsonPolicyDef(SUBSCRIPTIONS_LISTED)
    private String eventName;

    @JsonPolicyDef(SUBSCRIPTIONS_LISTED)
    private Set<String> names;

    public Filter() {
    }

    public HivePrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(HivePrincipal principal) {
        this.principal = principal;
    }

    public boolean isGlobal() {
        return global;
    }

    public void setGlobal(boolean global) {
        this.global = global;
    }

    public Set<Long> getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(Set<Long> networkIds) {
        this.networkIds = networkIds;
    }

    public Set<String> getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(Set<String> deviceIds) {
        this.deviceIds = deviceIds;
    }

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    @Override
    public String toString() {
        return "Filter{" +
                "principal=" + principal +
                ", global=" + global +
                ", networkIds=" + networkIds +
                ", deviceIds=" + deviceIds +
                ", eventName=" + eventName +
                ", names=" + names +
                '}';
    }
}

package com.devicehive.model;

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

import com.devicehive.json.strategies.JsonPolicyDef;

import java.util.Date;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.SUBSCRIPTIONS_LISTED;

public class SubscriptionInfo {

    @JsonPolicyDef(SUBSCRIPTIONS_LISTED)
    private Long subscriptionId;

    @JsonPolicyDef(SUBSCRIPTIONS_LISTED)
    private String type;

    @JsonPolicyDef(SUBSCRIPTIONS_LISTED)
    private String deviceId;

    @JsonPolicyDef(SUBSCRIPTIONS_LISTED)
    private Set<Long> networkIds;

    @JsonPolicyDef(SUBSCRIPTIONS_LISTED)
    private Set<Long> deviceTypeIds;

    @JsonPolicyDef(SUBSCRIPTIONS_LISTED)
    private Set<String> names;

    @JsonPolicyDef(SUBSCRIPTIONS_LISTED)
    private Date timestamp;

    public SubscriptionInfo(Long subscriptionId, String type, String deviceId, Set<Long> networkIds, Set<Long> deviceTypeIds, Set<String> names, Date timestamp) {
        this.subscriptionId = subscriptionId;
        this.type = type;
        this.deviceId = deviceId;
        this.networkIds = networkIds;
        this.deviceTypeIds = deviceTypeIds;
        this.names = names;
        this.timestamp = timestamp;
    }

    public SubscriptionInfo(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Set<Long> getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(Set<Long> networkIds) {
        this.networkIds = networkIds;
    }

    public Set<Long> getDeviceTypeIds() {
        return deviceTypeIds;
    }

    public void setDeviceTypeIds(Set<Long> deviceTypeIds) {
        this.deviceTypeIds = deviceTypeIds;
    }

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubscriptionInfo that = (SubscriptionInfo) o;

        return subscriptionId.equals(that.subscriptionId);

    }

    @Override
    public int hashCode() {
        return subscriptionId.hashCode();
    }

    @Override
    public String toString() {
        return "SubscriptionInfo{" +
                "subscriptionId=" + subscriptionId +
                ", type='" + type + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", networkIds=" + networkIds +
                ", deviceTypeIds=" + deviceTypeIds +
                ", names=" + names +
                ", timestamp=" + timestamp +
                '}';
    }
}

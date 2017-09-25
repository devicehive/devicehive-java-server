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
import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.SUBSCRIPTIONS_LISTED;

public class Filter implements Portable {

    public static final int FACTORY_ID = 1;
    public static final int CLASS_ID = 4;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Filter)) return false;
        Filter that = (Filter) o;
        return Objects.equals(principal, that.principal) &&
                Objects.equals(global, that.global) &&
                Objects.equals(networkIds, that.networkIds) &&
                Objects.equals(deviceIds, that.deviceIds) &&
                Objects.equals(eventName, that.eventName) &&
                Objects.equals(names, that.names);
    }

    @Override
    public int hashCode() {
        return Objects.hash(principal, global, networkIds, deviceIds, eventName, names);
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

    @Override
    public int getFactoryId() {
        return FACTORY_ID;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void writePortable(PortableWriter writer) throws IOException {
        writer.writePortable("principal", principal != null ? principal : new HivePrincipal());
        writer.writeBoolean("global", global);
        writer.writeLongArray("networkIds", networkIds != null ? networkIds.stream().mapToLong(Long::longValue).toArray() : new long[0]);
        writer.writeUTFArray("deviceIds", deviceIds != null ? deviceIds.toArray(new String[deviceIds.size()]) : new String[0]);
        writer.writeUTF("eventName", eventName);
        writer.writeUTFArray("names", names != null ? names.toArray(new String[names.size()]) : new String[0]);
    }

    @Override
    public void readPortable(PortableReader reader) throws IOException {
        principal = reader.readPortable("principal");
        global = reader.readBoolean("global");
        networkIds = Arrays.stream(reader.readLongArray("networkIds")).boxed().collect(Collectors.toSet());
        deviceIds = Arrays.stream(reader.readUTFArray("deviceIds")).collect(Collectors.toSet());
        eventName = reader.readUTF("eventName");
        names = Arrays.stream(reader.readUTFArray("names")).collect(Collectors.toSet());
    }
}

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

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import java.io.IOException;
import java.util.Objects;
import java.util.StringJoiner;

public class Filter implements Portable {

    public static final int FACTORY_ID = 1;
    public static final int CLASS_ID = 4;

    private Long networkId;

    private Long deviceTypeId;

    private String deviceId;

    private String eventName;

    private String name;

    public Filter() {

    }

    public Filter(Long networkId, Long deviceTypeId, String deviceId, String eventName, String name) {
        this.networkId = networkId;
        this.deviceTypeId = deviceTypeId;
        this.deviceId = deviceId;
        this.eventName = eventName;
        this.name = name;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public Long getDeviceTypeId() {
        return deviceTypeId;
    }

    public void setDeviceTypeId(Long deviceTypeId) {
        this.deviceTypeId = deviceTypeId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstKey() {
        StringJoiner joiner = new StringJoiner(",");

        joiner.add(networkId != null ? networkId.toString() : "*")
                .add(deviceTypeId != null ? deviceTypeId.toString() : "*")
                .add(deviceId != null ? deviceId : "*");

        return joiner.toString();
    }

    public String getDeviceIgnoredFirstKey() {
        StringJoiner joiner = new StringJoiner(",");

        joiner.add(networkId != null ? networkId.toString() : "*")
                .add(deviceTypeId != null ? deviceTypeId.toString() : "*")
                .add("*");

        return joiner.toString();
    }

    public String getSecondKey() {
        StringJoiner joiner = new StringJoiner(",");

        joiner.add(eventName != null ? eventName : "*")
                .add(name != null ? name : "*");

        return joiner.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Filter)) return false;
        Filter that = (Filter) o;
        return Objects.equals(networkId, that.networkId) &&
                Objects.equals(deviceTypeId, that.deviceTypeId) &&
                Objects.equals(deviceId, that.deviceId) &&
                Objects.equals(eventName, that.eventName) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId, deviceTypeId, deviceId, eventName, name);
    }

    @Override
    public String toString() {
        return "Filter{" +
                "networkId=" + networkId +
                ", deviceTypeId=" + deviceTypeId +
                ", deviceId=" + deviceId +
                ", eventName=" + eventName +
                ", name=" + name +
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
        writer.writeLong("networkId", Objects.nonNull(networkId) ? networkId : 0);
        writer.writeLong("deviceTypeId", Objects.nonNull(deviceTypeId) ? deviceTypeId : 0);
        writer.writeUTF("deviceId", deviceId);
        writer.writeUTF("eventName", eventName);
        writer.writeUTF("name", name);
    }

    @Override
    public void readPortable(PortableReader reader) throws IOException {
        networkId = reader.readLong("networkId");
        deviceTypeId = reader.readLong("deviceTypeId");
        deviceId = reader.readUTF("deviceId");
        eventName = reader.readUTF("eventName");
        name = reader.readUTF("name");
    }
}

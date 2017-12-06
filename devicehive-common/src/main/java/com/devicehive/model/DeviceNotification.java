package com.devicehive.model;

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
import com.google.gson.annotations.SerializedName;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceNotification implements HiveEntity, HazelcastEntity, Portable {
    private static final long serialVersionUID = 1834383778016225837L;
    private transient HazelcastInstance hazelcastInstance;
    public static final int FACTORY_ID = 1;
    public static final int CLASS_ID = 1;
    
    @SerializedName("id")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Long id;

    @SerializedName("notification")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private String notification;

    @SerializedName("deviceId")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private String deviceId;

    @SerializedName("networkId")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Long networkId;

    @SerializedName("deviceTypeId")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Long deviceTypeId;

    @SerializedName("timestamp")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @SerializedName("parameters")
    @JsonPolicyDef({NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_CLIENT})
    private JsonStringWrapper parameters;

    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceNotification message = (DeviceNotification) o;

        if (deviceId != null ? !deviceId.equals(message.deviceId) : message.deviceId != null) return false;
        if (networkId != null ? !networkId.equals(message.networkId) : message.networkId != null) return false;
        if (deviceTypeId != null ? !deviceTypeId.equals(message.deviceTypeId) : message.deviceTypeId != null) return false;
        if (id != null ? !id.equals(message.id) : message.id != null) return false;
        if (notification != null ? !notification.equals(message.notification) : message.notification != null)
            return false;
        if (parameters != null ? !parameters.equals(message.parameters) : message.parameters != null) return false;
        if (timestamp != null ? !timestamp.equals(message.timestamp) : message.timestamp != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (notification != null ? notification.hashCode() : 0);
        result = 31 * result + (deviceId != null ? deviceId.hashCode() : 0);
        result = 31 * result + (networkId != null ? networkId.hashCode() : 0);
        result = 31 * result + (deviceTypeId != null ? deviceTypeId.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeviceNotification{" +
                "id=" + id +
                ", notification='" + notification + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", networkId='" + networkId + '\'' +
                ", deviceTypeId='" + deviceTypeId + '\'' +
                ", timestamp=" + timestamp +
                ", parameters='" + parameters + '\'' +
                '}';
    }

    @Override
    @ApiModelProperty(hidden = true)
    public String getHazelcastKey() {
        return id+"-"+deviceId+"-"+timestamp;
    }

    @Override
    @ApiModelProperty(hidden = true)
    public int getFactoryId() {
        return FACTORY_ID;
    }

    @Override
    @ApiModelProperty(hidden = true)
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void writePortable(PortableWriter portableWriter) throws IOException {
        portableWriter.writeLong("id", Objects.nonNull(id) ? id : 0);
        portableWriter.writeUTF("notification", notification);
        portableWriter.writeUTF("deviceId", deviceId);
        portableWriter.writeLong("networkId", Objects.nonNull(networkId) ? networkId : 0);
        portableWriter.writeLong("deviceTypeId", Objects.nonNull(deviceTypeId) ? deviceTypeId : 0);
        portableWriter.writeLong("timestamp", Objects.nonNull(timestamp) ? timestamp.getTime() :0);
        boolean parametersIsNotNull = Objects.nonNull(parameters) && Objects.nonNull(parameters.getJsonString());
        portableWriter.writeUTF("parameters", parametersIsNotNull ? parameters.getJsonString() : null);
    }

    @Override
    public void readPortable(PortableReader portableReader) throws IOException {
        id = portableReader.readLong("id");
        notification = portableReader.readUTF("notification");
        deviceId = portableReader.readUTF("deviceId");
        networkId = portableReader.readLong("networkId");
        deviceTypeId = portableReader.readLong("deviceTypeId");
        timestamp = new Date(portableReader.readLong("timestamp"));
        String parametersString = portableReader.readUTF("parameters");
        if (Objects.nonNull(parametersString)) {
            parameters = new JsonStringWrapper(parametersString);
        }
    }

    @Override
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }
}

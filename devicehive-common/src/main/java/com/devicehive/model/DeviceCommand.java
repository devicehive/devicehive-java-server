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

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Created by tmatvienko on 1/27/15.
 */
public class DeviceCommand implements HiveEntity, HazelcastEntity, Portable {
    private static final long serialVersionUID = 4140545193474112756L;
    private transient HazelcastInstance hazelcastInstance;
    public static final int FACTORY_ID = 1;
    public static final int CLASS_ID = 2;

    @SerializedName("id")
    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, POST_COMMAND_TO_DEVICE,
            COMMAND_LISTED})
    private Long id;

    @SerializedName("command")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private String command;

    @SerializedName("timestamp")
    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @SerializedName("lastUpdated")
    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdated;

    @SerializedName("userId")
    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_LISTED})
    private Long userId;

    @SerializedName("deviceId")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private String deviceId;

    @SerializedName("networkId")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private Long networkId;

    @SerializedName("deviceTypeId")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private Long deviceTypeId;

    @SerializedName("parameters")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private JsonStringWrapper parameters;

    @SerializedName("lifetime")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            COMMAND_LISTED})
    private Integer lifetime;

    @SerializedName("status")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private String status;

    @SerializedName("result")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private JsonStringWrapper result;

    @SerializedName("isUpdated")
    @ApiModelProperty(hidden = true)
    private Boolean isUpdated;

    public DeviceCommand() {
    }

    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    public Integer getLifetime() {
        return lifetime;
    }

    public void setLifetime(Integer lifetime) {
        this.lifetime = lifetime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public JsonStringWrapper getResult() {
        return result;
    }

    public void setResult(JsonStringWrapper result) {
        this.result = result;
    }

    public Boolean getIsUpdated() {
        return isUpdated;
    }

    public void setIsUpdated(Boolean isUpdated) {
        this.isUpdated = isUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceCommand message = (DeviceCommand) o;

        if (command != null ? !command.equals(message.command) : message.command != null) return false;
        if (deviceId != null ? !deviceId.equals(message.deviceId) : message.deviceId != null) return false;
        if (networkId != null ? !networkId.equals(message.networkId) : message.networkId != null) return false;
        if (deviceTypeId != null ? !deviceTypeId.equals(message.deviceTypeId) : message.deviceTypeId != null) return false;
        if (id != null ? !id.equals(message.id) : message.id != null) return false;
        if (isUpdated != null ? !isUpdated.equals(message.isUpdated) : message.isUpdated != null) return false;
        if (lifetime != null ? !lifetime.equals(message.lifetime) : message.lifetime != null) return false;
        if (parameters != null ? !parameters.equals(message.parameters) : message.parameters != null) return false;
        if (result != null ? !result.equals(message.result) : message.result != null) return false;
        if (status != null ? !status.equals(message.status) : message.status != null) return false;
        if (timestamp != null ? !timestamp.equals(message.timestamp) : message.timestamp != null) return false;
        if (userId != null ? !userId.equals(message.userId) : message.userId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result1 = id != null ? id.hashCode() : 0;
        result1 = 31 * result1 + (command != null ? command.hashCode() : 0);
        result1 = 31 * result1 + (timestamp != null ? timestamp.hashCode() : 0);
        result1 = 31 * result1 + (userId != null ? userId.hashCode() : 0);
        result1 = 31 * result1 + (deviceId != null ? deviceId.hashCode() : 0);
        result1 = 31 * result1 + (networkId != null ? networkId.hashCode() : 0);
        result1 = 31 * result1 + (deviceTypeId != null ? deviceTypeId.hashCode() : 0);
        result1 = 31 * result1 + (parameters != null ? parameters.hashCode() : 0);
        result1 = 31 * result1 + (lifetime != null ? lifetime.hashCode() : 0);
        result1 = 31 * result1 + (status != null ? status.hashCode() : 0);
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        result1 = 31 * result1 + (isUpdated != null ? isUpdated.hashCode() : 0);
        return result1;
    }

    @Override
    public String toString() {
        return "DeviceCommand{" +
                "id=" + id +
                ", command='" + command + '\'' +
                ", timestamp=" + timestamp +
                ", lastUpdated=" + lastUpdated +
                ", userId=" + userId +
                ", deviceId='" + deviceId + '\'' +
                ", networkId='" + networkId + '\'' +
                ", deviceTypeId='" + deviceTypeId + '\'' +
                ", parameters=" + parameters +
                ", lifetime=" + lifetime +
                ", status='" + status + '\'' +
                ", result=" + result +
                ", isUpdated=" + isUpdated +
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
        portableWriter.writeUTF("command", command);
        portableWriter.writeLong("timestamp", Objects.nonNull(timestamp) ? timestamp.getTime() :0);
        portableWriter.writeLong("lastUpdated", Objects.nonNull(lastUpdated) ? lastUpdated.getTime() :0);
        portableWriter.writeLong("userId", Objects.nonNull(userId) ? userId : 0);
        portableWriter.writeUTF("deviceId", deviceId);
        portableWriter.writeLong("networkId", Objects.nonNull(networkId) ? networkId : 0);
        portableWriter.writeLong("deviceTypeId", Objects.nonNull(deviceTypeId) ? deviceTypeId : 0);
        boolean parametersIsNotNull = Objects.nonNull(parameters) && Objects.nonNull(parameters.getJsonString());
        portableWriter.writeUTF("parameters", parametersIsNotNull ? parameters.getJsonString() : null);
        portableWriter.writeInt("lifetime", Objects.nonNull(lifetime) ? lifetime : 0);
        portableWriter.writeUTF("status", status);
        boolean resultIsNotNull = Objects.nonNull(result) && Objects.nonNull(result.getJsonString());
        portableWriter.writeUTF("result", resultIsNotNull ? result.getJsonString() : null);
        portableWriter.writeBoolean("isUpdated", Objects.nonNull(isUpdated)? isUpdated : false);
    }

    @Override
    public void readPortable(PortableReader portableReader) throws IOException {
        id = portableReader.readLong("id");
        command = portableReader.readUTF("command");
        timestamp = new Date(portableReader.readLong("timestamp"));
        lastUpdated = new Date(portableReader.readLong("lastUpdated"));
        userId = portableReader.readLong("userId");
        deviceId = portableReader.readUTF("deviceId");
        networkId = portableReader.readLong("networkId");
        deviceTypeId = portableReader.readLong("deviceTypeId");
        String parametersString = portableReader.readUTF("parameters");
        if (Objects.nonNull(parametersString)) {
            parameters = new JsonStringWrapper(parametersString);
        }
        lifetime = portableReader.readInt("lifetime");
        status = portableReader.readUTF("status");
        String resultString = portableReader.readUTF("result");
        if (Objects.nonNull(resultString)) {
            result = new JsonStringWrapper(resultString);
        }
        isUpdated = portableReader.readBoolean("isUpdated");
    }

    @Override
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }
}
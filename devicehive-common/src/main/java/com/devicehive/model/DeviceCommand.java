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

import com.devicehive.configuration.Constants;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serial;
import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_FROM_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_UPDATE_FROM_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_UPDATE_TO_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.POST_COMMAND_TO_DEVICE;

/**
 * Created by tmatvienko on 1/27/15.
 */
public class DeviceCommand implements HiveEntity, CacheEntity {

    @Serial
    private static final long serialVersionUID = 4140545193474112756L;

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
    public boolean equals(final Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        final DeviceCommand that = (DeviceCommand) o;

        return new EqualsBuilder().append(getId(), that.getId())
                                  .append(getCommand(), that.getCommand())
                                  .append(getTimestamp(), that.getTimestamp())
                                  .append(getLastUpdated(), that.getLastUpdated())
                                  .append(getUserId(), that.getUserId())
                                  .append(getDeviceId(), that.getDeviceId())
                                  .append(getNetworkId(), that.getNetworkId())
                                  .append(getDeviceTypeId(), that.getDeviceTypeId())
                                  .append(getParameters(), that.getParameters())
                                  .append(getLifetime(), that.getLifetime())
                                  .append(getStatus(), that.getStatus())
                                  .append(getResult(), that.getResult())
                                  .append(getIsUpdated(), that.getIsUpdated())
                                  .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getId())
                                          .append(getCommand())
                                          .append(getTimestamp())
                                          .append(getLastUpdated())
                                          .append(getUserId())
                                          .append(getDeviceId())
                                          .append(getNetworkId())
                                          .append(getDeviceTypeId())
                                          .append(getParameters())
                                          .append(getLifetime())
                                          .append(getStatus())
                                          .append(getResult())
                                          .append(getIsUpdated())
                                          .toHashCode();
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

    @JsonIgnore
    @Override
    public String getCacheKey() {
        return String.format("%s_%s_%s", Constants.COMMANDS, deviceId, id);
    }
}
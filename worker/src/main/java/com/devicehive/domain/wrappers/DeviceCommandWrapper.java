package com.devicehive.domain.wrappers;

import com.devicehive.domain.JsonStringWrapper;
import com.devicehive.messages.converter.adapter.TimestampAdapter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Created by tmatvienko on 2/24/15.
 */
public class DeviceCommandWrapper implements Serializable {

    @SerializedName("id")
    private Long id;

    @SerializedName("deviceGuid")
    private String deviceGuid;

    @SerializedName("timestamp")
    private Timestamp timestamp;

    @SerializedName("command")
    private String command;

    @SerializedName("parameters")
    private JsonStringWrapper parameters;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("lifetime")
    private Integer lifetime;

    @SerializedName("flags")
    private Integer flags;

    @SerializedName("status")
    private String status;

    @SerializedName("result")
    private JsonStringWrapper result;

    @SerializedName("isUpdated")
    private Boolean isUpdated;

    public DeviceCommandWrapper(){}

    public Long getId() {
        return id;
    }

    public String getDeviceGuid() {
        return deviceGuid;
    }

    @JsonSerialize(using = TimestampAdapter.class)
    public Timestamp getTimestamp() {
         return timestamp;
    }

    public String getCommand() {
        return command;
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getLifetime() {
        return lifetime;
    }

    public Integer getFlags() {
        return flags;
    }

    public String getStatus() {
        return status;
    }

    public JsonStringWrapper getResult() {
        return result;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDeviceGuid(String deviceGuid) {
        this.deviceGuid = deviceGuid;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setLifetime(Integer lifetime) {
        this.lifetime = lifetime;
    }

    public void setFlags(Integer flags) {
        this.flags = flags;
    }

    public void setStatus(String status) {
        this.status = status;
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

        DeviceCommandWrapper that = (DeviceCommandWrapper) o;

        if (command != null ? !command.equals(that.command) : that.command != null) return false;
        if (deviceGuid != null ? !deviceGuid.equals(that.deviceGuid) : that.deviceGuid != null) return false;
        if (flags != null ? !flags.equals(that.flags) : that.flags != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (isUpdated != null ? !isUpdated.equals(that.isUpdated) : that.isUpdated != null) return false;
        if (lifetime != null ? !lifetime.equals(that.lifetime) : that.lifetime != null) return false;
        if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) return false;
        if (result != null ? !result.equals(that.result) : that.result != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result1 = id != null ? id.hashCode() : 0;
        result1 = 31 * result1 + (deviceGuid != null ? deviceGuid.hashCode() : 0);
        result1 = 31 * result1 + (timestamp != null ? timestamp.hashCode() : 0);
        result1 = 31 * result1 + (command != null ? command.hashCode() : 0);
        result1 = 31 * result1 + (parameters != null ? parameters.hashCode() : 0);
        result1 = 31 * result1 + (userId != null ? userId.hashCode() : 0);
        result1 = 31 * result1 + (lifetime != null ? lifetime.hashCode() : 0);
        result1 = 31 * result1 + (flags != null ? flags.hashCode() : 0);
        result1 = 31 * result1 + (status != null ? status.hashCode() : 0);
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        result1 = 31 * result1 + (isUpdated != null ? isUpdated.hashCode() : 0);
        return result1;
    }

    @Override
    public String toString() {
        return "DeviceCommandWrapper{" +
                "id=" + id +
                ", deviceGuid='" + deviceGuid + '\'' +
                ", timestamp=" + timestamp +
                ", command='" + command + '\'' +
                ", parameters=" + parameters +
                ", userId=" + userId +
                ", lifetime=" + lifetime +
                ", flags=" + flags +
                ", status='" + status + '\'' +
                ", result=" + result +
                ", isUpdated=" + isUpdated +
                '}';
    }
}

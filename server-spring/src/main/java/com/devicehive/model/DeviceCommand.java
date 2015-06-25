package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Created by tmatvienko on 1/27/15.
 */
public class DeviceCommand implements HiveEntity  {
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
    private Timestamp timestamp;

    @SerializedName("userId")
    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_LISTED})
    private Long userId;

    @SerializedName("deviceGuid")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private String deviceGuid;

    @SerializedName("parameters")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private JsonStringWrapper parameters;

    @SerializedName("lifetime")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            COMMAND_LISTED})
    private Integer lifetime;

    @SerializedName("flags")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            REST_COMMAND_UPDATE_FROM_DEVICE, COMMAND_LISTED})
    private Integer flags;

    @SerializedName("status")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, REST_COMMAND_UPDATE_FROM_DEVICE, COMMAND_LISTED})
    private String status;

    @SerializedName("result")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, REST_COMMAND_UPDATE_FROM_DEVICE, COMMAND_LISTED})
    private JsonStringWrapper result;

    @SerializedName("isUpdated")
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

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = new Timestamp(timestamp);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDeviceGuid() {
        return deviceGuid;
    }

    public void setDeviceGuid(String deviceGuid) {
        this.deviceGuid = deviceGuid;
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

    public Integer getFlags() {
        return flags;
    }

    public void setFlags(Integer flags) {
        this.flags = flags;
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
        if (deviceGuid != null ? !deviceGuid.equals(message.deviceGuid) : message.deviceGuid != null) return false;
        if (flags != null ? !flags.equals(message.flags) : message.flags != null) return false;
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
        result1 = 31 * result1 + (deviceGuid != null ? deviceGuid.hashCode() : 0);
        result1 = 31 * result1 + (parameters != null ? parameters.hashCode() : 0);
        result1 = 31 * result1 + (lifetime != null ? lifetime.hashCode() : 0);
        result1 = 31 * result1 + (flags != null ? flags.hashCode() : 0);
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
                ", userId=" + userId +
                ", deviceGuid='" + deviceGuid + '\'' +
                ", parameters=" + parameters +
                ", lifetime=" + lifetime +
                ", flags=" + flags +
                ", status='" + status + '\'' +
                ", result=" + result +
                ", isUpdated=" + isUpdated +
                '}';
    }
}

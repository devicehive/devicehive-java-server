package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Created by tmatvienko on 1/27/15.
 */
public class DeviceCommandMessage implements HiveEntity  {
    private static final long serialVersionUID = 2234945156674112745L;

    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
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

    @SerializedName("device_guid")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private String deviceGuid;

    @SerializedName("parameters")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private JsonObject parameters;

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
            POST_COMMAND_TO_DEVICE,
            REST_COMMAND_UPDATE_FROM_DEVICE, COMMAND_LISTED})
    private JsonObject result;

    private String originSessionId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public JsonObject getParameters() {
        return parameters;
    }

    public void setParameters(JsonObject parameters) {
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

    public JsonObject getResult() {
        return result;
    }

    public void setResult(JsonObject result) {
        this.result = result;
    }

    public String getOriginSessionId() {
        return originSessionId;
    }

    public void setOriginSessionId(String originSessionId) {
        this.originSessionId = originSessionId;
    }

    @Override
    public String toString() {
        return "DeviceCommandMessage{" +
                "id='" + id + '\'' +
                ", command='" + command + '\'' +
                ", timestamp=" + timestamp +
                ", userId=" + userId +
                ", deviceGuid='" + deviceGuid + '\'' +
                ", parameters=" + parameters +
                ", lifetime=" + lifetime +
                ", flags=" + flags +
                ", status='" + status + '\'' +
                ", result=" + result +
                ", originSessionId='" + originSessionId + '\'' +
                '}';
    }
}

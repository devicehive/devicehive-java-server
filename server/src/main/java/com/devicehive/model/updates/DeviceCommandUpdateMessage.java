package com.devicehive.model.updates;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_UPDATE_FROM_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_COMMAND_UPDATE_FROM_DEVICE;

/**
 * Created by tmatvienko on 1/30/15.
 */
public class DeviceCommandUpdateMessage implements HiveEntity {
    private static final long serialVersionUID = 1258723517719841194L;

    @SerializedName("id")
    @JsonPolicyDef({COMMAND_UPDATE_FROM_DEVICE})
    private Long id;

    @SerializedName("timestamp")
    @JsonPolicyDef({REST_COMMAND_UPDATE_FROM_DEVICE, COMMAND_UPDATE_FROM_DEVICE})
    private Timestamp timestamp;

    @SerializedName("command")
    @JsonPolicyDef({REST_COMMAND_UPDATE_FROM_DEVICE, COMMAND_UPDATE_FROM_DEVICE})
    private String command;

    @SerializedName("parameters")
    @JsonPolicyDef({REST_COMMAND_UPDATE_FROM_DEVICE, COMMAND_UPDATE_FROM_DEVICE})
    private JsonObject parameters;

    @SerializedName("lifetime")
    @JsonPolicyDef({REST_COMMAND_UPDATE_FROM_DEVICE, COMMAND_UPDATE_FROM_DEVICE})
    private Integer lifetime;

    @SerializedName("flags")
    @JsonPolicyDef({REST_COMMAND_UPDATE_FROM_DEVICE, COMMAND_UPDATE_FROM_DEVICE})
    private Integer flags;

    @SerializedName("result")
    @JsonPolicyDef({REST_COMMAND_UPDATE_FROM_DEVICE, COMMAND_UPDATE_FROM_DEVICE})
    private JsonObject result;

    @SerializedName("status")
    @JsonPolicyDef({REST_COMMAND_UPDATE_FROM_DEVICE, COMMAND_UPDATE_FROM_DEVICE})
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
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

    public JsonObject getResult() {
        return result;
    }

    public void setResult(JsonObject result) {
        this.result = result;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "DeviceCommandUpdateMessage{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", command='" + command + '\'' +
                ", parameters=" + parameters +
                ", lifetime=" + lifetime +
                ", flags=" + flags +
                ", result=" + result +
                ", status='" + status + '\'' +
                '}';
    }
}

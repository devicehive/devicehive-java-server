package com.devicehive.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.Date;


/**
 * TODO JavaDoc
 */
public class DeviceCommand {

    @SerializedName("id")
    public Integer id;

    @SerializedName("timestamp")
    public Date timestamp;

    @SerializedName("userId")
    public Integer userId;

    @SerializedName("command")
    public String command;

    @SerializedName("parameters")
    public JsonElement parameters;

    @SerializedName("lifetime")
    public Integer lifetime;

    @SerializedName("flags")
    public Integer flags;

    @SerializedName("status")
    public String status;

    @SerializedName("result")
    public JsonElement result;

    public DeviceCommand() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public JsonElement getParameters() {
        return parameters;
    }

    public void setParameters(JsonElement parameters) {
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

    public JsonElement getResult() {
        return result;
    }

    public void setResult(JsonElement result) {
        this.result = result;
    }
}

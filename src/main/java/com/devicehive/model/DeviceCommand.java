package com.devicehive.model;

import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;

/**
 * TODO JavaDoc
 */
public class DeviceCommand {

    @SerializedName("id")
    public Integer id;

    public Timestamp timestamp;
    public Integer userId;
    public String command;
    public Object parameters;
    public Integer lifetime;

    @SerializedName("id")
    public Integer flags;
    public String status;
    public Object result;

    public DeviceCommand() {
    }
}

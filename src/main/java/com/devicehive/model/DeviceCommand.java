package com.devicehive.model;

import java.sql.Timestamp;

/**
 * TODO JavaDoc
 */
public class DeviceCommand {
    public Integer id;
    public Timestamp timestamp;
    public Integer userId;
    public String command;
    public Object parameters;
    public Integer lifetime;
    public Integer flags;
    public String status;
    public Object result;

    public DeviceCommand() {
    }
}

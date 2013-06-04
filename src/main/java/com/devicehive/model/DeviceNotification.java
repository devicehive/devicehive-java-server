package com.devicehive.model;

import java.sql.Timestamp;

/**
 * TODO JavaDoc
 */
public class DeviceNotification {
    public Integer id;
    public Timestamp timestamp;
    public String notification;
    public Object parameters;

    public DeviceNotification() {
    }
}

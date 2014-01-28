package com.devicehive.client.model;


import com.devicehive.client.model.exceptions.InternalHiveClientException;

/**
 * Enumeration of all allowed actions for access keys
 */
public enum AllowedAction {
    GET_NETWORK("GetNetwork"),
    GET_DEVICE("GetDevice"),
    GET_DEVICE_STATE("GetDeviceState"),
    GET_DEVICE_NOTIFICATION("GetDeviceNotification"),
    GET_DEVICE_COMMAND("GetDeviceCommand"),
    REGISTER_DEVICE("RegisterDevice"),
    CREATE_DEVICE_NOTIFICATION("CreateDeviceNotification"),
    CREATE_DEVICE_COMMAND("CreateDeviceCommand"),
    UPDATE_DEVICE_COMMAND("UpdateDeviceCommand");
    private final String value;

    AllowedAction(String value) {
        this.value = value;
    }


    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.name();
    }
}

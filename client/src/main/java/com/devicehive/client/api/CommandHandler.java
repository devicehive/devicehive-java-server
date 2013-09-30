package com.devicehive.client.api;


import com.devicehive.client.model.DeviceCommand;

public interface CommandHandler {

    void handle(DeviceCommand command);
}

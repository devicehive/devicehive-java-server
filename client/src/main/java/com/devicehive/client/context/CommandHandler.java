package com.devicehive.client.context;


import com.devicehive.client.model.DeviceCommand;

public interface CommandHandler {

    void handle(DeviceCommand command);
}

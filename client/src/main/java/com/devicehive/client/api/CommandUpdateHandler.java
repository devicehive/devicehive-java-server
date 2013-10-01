package com.devicehive.client.api;


import com.devicehive.client.model.DeviceCommand;

public interface CommandUpdateHandler {

    void handle(DeviceCommand command);

}

package com.devicehive.client.context;


import com.devicehive.client.model.DeviceCommand;

public interface CommandUpdateHandler {

    void handle(DeviceCommand command);

}

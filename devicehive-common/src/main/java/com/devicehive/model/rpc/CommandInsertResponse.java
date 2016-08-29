package com.devicehive.model.rpc;

import com.devicehive.model.DeviceCommand;
import com.devicehive.shim.api.Body;

public class CommandInsertResponse extends Body {

    private DeviceCommand deviceCommand;

    public CommandInsertResponse(DeviceCommand deviceCommand) {
        super(Action.COMMAND_INSERT_RESPONSE.name());
        this.deviceCommand = deviceCommand;
    }

    public DeviceCommand getDeviceCommand() {
        return deviceCommand;
    }

    public void setDeviceCommand(DeviceCommand deviceCommand) {
        this.deviceCommand = deviceCommand;
    }
}

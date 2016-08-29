package com.devicehive.model.rpc;

import com.devicehive.model.DeviceCommand;
import com.devicehive.shim.api.Body;

public class CommandInsertRequest extends Body {

    private DeviceCommand deviceCommand;

    public CommandInsertRequest(DeviceCommand deviceCommand) {
        super(Action.COMMAND_INSERT_REQUEST.name());
        this.deviceCommand = deviceCommand;
    }

    public DeviceCommand getDeviceCommand() {
        return deviceCommand;
    }

    public void setDeviceCommand(DeviceCommand deviceCommand) {
        this.deviceCommand = deviceCommand;
    }
}

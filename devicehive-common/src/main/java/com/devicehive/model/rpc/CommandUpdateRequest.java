package com.devicehive.model.rpc;

import com.devicehive.model.DeviceCommand;
import com.devicehive.shim.api.Body;

public class CommandUpdateRequest extends Body {

    private DeviceCommand deviceCommand;

    public CommandUpdateRequest(DeviceCommand deviceCommand) {
        super(Action.COMMAND_UPDATE_REQUEST.name());
        this.deviceCommand = deviceCommand;
    }

    public DeviceCommand getDeviceCommand() {
        return deviceCommand;
    }
}

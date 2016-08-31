package com.devicehive.model.rpc;

import com.devicehive.model.DeviceCommand;
import com.devicehive.shim.api.Body;

public class CommandUpdateSubscribeResponse extends Body {

    private String subscriptionId;
    private DeviceCommand deviceCommand;

    public CommandUpdateSubscribeResponse(String subscriptionId, DeviceCommand deviceCommand) {
        super(Action.COMMAND_UPDATE_SUBSCRIBE_RESPONSE.name());
        this.subscriptionId = subscriptionId;
        this.deviceCommand = deviceCommand;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public DeviceCommand getDeviceCommand() {
        return deviceCommand;
    }
}

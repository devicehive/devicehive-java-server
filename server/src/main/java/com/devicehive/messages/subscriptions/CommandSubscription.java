package com.devicehive.messages.subscriptions;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.messages.handler.HandlerCreator;
import com.devicehive.model.DeviceCommand;

import java.util.UUID;

public class CommandSubscription extends Subscription<String, DeviceCommand> {

    private final HivePrincipal principal;
    private final String commandNames;

    public CommandSubscription(HivePrincipal principal, String guid, UUID subscriptionId,
                               String commandNames,
                               HandlerCreator<DeviceCommand> handlerCreator) {
        super(guid, subscriptionId, handlerCreator);
        this.principal = principal;
        this.commandNames = commandNames;
    }

    public String getDeviceGuid() {
        return getEventSource();
    }

    public HivePrincipal getPrincipal() {
        return principal;
    }

    public String getCommandNames() {
        return commandNames;
    }

}


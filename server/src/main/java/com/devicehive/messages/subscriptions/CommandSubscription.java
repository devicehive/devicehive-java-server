package com.devicehive.messages.subscriptions;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.messages.handler.HandlerCreator;
import com.devicehive.model.DeviceCommandMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CommandSubscription extends Subscription<String, DeviceCommandMessage> {

    private final HivePrincipal principal;
    private final Set<String> commandNames;

    public CommandSubscription(HivePrincipal principal, String guid, UUID subscriptionId,
                               Collection<String> commandNames,
                               HandlerCreator<DeviceCommandMessage> handlerCreator) {
        super(guid, subscriptionId, handlerCreator);
        this.principal = principal;
        this.commandNames = commandNames != null ? new HashSet<>(commandNames) : null;
    }

    public String getDeviceGuid() {
        return getEventSource();
    }

    public HivePrincipal getPrincipal() {
        return principal;
    }

    public Set<String> getCommandNames() {
        return commandNames;
    }

}


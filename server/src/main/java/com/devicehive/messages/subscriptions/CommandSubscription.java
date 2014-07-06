package com.devicehive.messages.subscriptions;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.messages.handler.HandlerCreator;
import com.devicehive.model.DeviceCommand;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CommandSubscription extends Subscription<Long,DeviceCommand> {

    private final HivePrincipal principal;
    private final Set<String> commandNames;

    public CommandSubscription(HivePrincipal principal, Long deviceId, UUID subscriptionId,
                               Collection<String> commandNames,
                               HandlerCreator<DeviceCommand>  handlerCreator) {
        super(deviceId, subscriptionId, handlerCreator);
        this.principal = principal;
        this.commandNames = commandNames != null ? new HashSet<>(commandNames) : null;
    }

    public Long getDeviceId() {
        return getEventSource();
    }

    public HivePrincipal getPrincipal() {
        return principal;
    }

    public Set<String> getCommandNames() {
        return commandNames;
    }

}


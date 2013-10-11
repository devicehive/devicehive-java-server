package com.devicehive.messages.subscriptions;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.messages.handler.HandlerCreator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CommandSubscription extends Subscription<Long> {

    private final HivePrincipal principal;
    private final Set<String> commandNames;

    public CommandSubscription(HivePrincipal principal, Long deviceId, String subscriberId, Collection<String> commandNames,
                               HandlerCreator handlerCreator) {
        super(deviceId, subscriberId, handlerCreator);
        this.principal = principal;
        this.commandNames = commandNames != null ? new HashSet<>(commandNames) : null;
    }

    public Long getDeviceId() {
        return getEventSource();
    }

    public String getSessionId() {
        return getSubscriberId();
    }

    public HivePrincipal getPrincipal(){
        return  principal;
    }

    public Set<String> getCommandNames() {
        return commandNames;
    }
}


package com.devicehive.messages.subscriptions;

import java.util.Set;

public class CommandUpdateSubscriptionStorage extends AbstractStorage<String, CommandSubscription> {

    public Set<CommandSubscription> getByDeviceGuid(String guid) {
        return get(guid);
    }

    public synchronized void removeByDeviceGuid(String guid) {
        removeByEventSource(guid);
    }
}

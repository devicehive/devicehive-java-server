package com.devicehive.messages.subscriptions;

import java.util.Set;

public class CommandUpdateSubscriptionStorage extends AbstractStorage<String, CommandUpdateSubscription> {

    public Set<CommandUpdateSubscription> getByCommandId(String id) {
        return get(id);
    }

    public synchronized void removeByCommandId(String commandId) {
        removeByEventSource(commandId);
    }

}

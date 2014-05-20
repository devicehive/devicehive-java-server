package com.devicehive.messages.subscriptions;

import java.util.Set;

public class CommandUpdateSubscriptionStorage extends AbstractStorage<Long, CommandUpdateSubscription> {

    public Set<CommandUpdateSubscription> getByCommandId(Long id) {
        return get(id);
    }


    public synchronized void removeByCommandId(Long commandId) {
        removeByEventSource(commandId);
    }

}

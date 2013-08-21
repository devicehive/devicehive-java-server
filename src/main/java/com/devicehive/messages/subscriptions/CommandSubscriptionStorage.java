package com.devicehive.messages.subscriptions;

import java.util.Set;

public class CommandSubscriptionStorage extends AbstractStorage<Long, CommandSubscription> {

    public Set<CommandSubscription> getByDeviceId(Long id) {
        return get(id);
    }


    public synchronized void removeBySession(String sessionId) {
        removeBySubscriber(sessionId);
    }

    public Set<CommandSubscription> getBySession(String sessionId) {
        return get(sessionId);
    }

    public synchronized void removeByDevice(Long deviceId) {
        removeByEventSource(deviceId);
    }

}

package com.devicehive.messages.subscriptions;

import java.util.Set;

public class NotificationSubscriptionStorage extends AbstractStorage<Long, NotificationSubscription> {

    public Set<NotificationSubscription> getByDeviceId(Long id) {
        return get(id);
    }


    public synchronized void removeBySession(String sessionId) {
        removeBySubscriber(sessionId);
    }

    public synchronized void removeByDevice(Long deviceId) {
        removeByEventSource(deviceId);
    }

}

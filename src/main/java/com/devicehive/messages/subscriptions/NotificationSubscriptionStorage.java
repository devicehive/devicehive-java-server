package com.devicehive.messages.subscriptions;

import java.util.Set;

public class NotificationSubscriptionStorage extends AbstractStorage<String, NotificationSubscription> {

    public Set<NotificationSubscription> getByDeviceGuid(String guid) {
        return get(guid);
    }

    public synchronized void removeByDevice(String deviceGuid) {
        removeByEventSource(deviceGuid);
    }

}

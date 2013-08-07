package com.devicehive.messages.subscriptions;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Set;

public class NotificationSubscriptionStorage extends AbstractStorage<Long, NotificationSubscription> {

    public Set<NotificationSubscription> getByDeviceId(Long id) {
        return get(id);
    }

    public synchronized void insert(Collection<NotificationSubscription> coll) {
        for (NotificationSubscription ns : coll) {
            insert(ns);
        }
    }

    public synchronized void remove(Collection<Pair<Long,String>> coll) {
        for (Pair<Long,String> pair : coll) {
            remove(pair.getKey(), pair.getValue());
        }
    }

    public synchronized void removeBySession(String sessionId) {
        removeBySubscriber(sessionId);
    }

    public synchronized void removeByDevice(Long deviceId) {
        removeByEventSource(deviceId);
    }

}

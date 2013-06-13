package com.devicehive.websockets.messagebus;


import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;



abstract class SimpleSubscriptionsManager<S> implements SubscriptionsManager<S> {

    private ConcurrentMap<UUID, Set<S>> deviceNotificationMap = new ConcurrentHashMap<UUID, Set<S>>();



    public SimpleSubscriptionsManager() {
    }


    public void subscribe(S clientSession, UUID... devices) {
        synchronized (clientSession) {
            for (UUID dev : devices) {
                synchronized (deviceNotificationMap) {
                    Set<S> set = Collections.newSetFromMap(new ConcurrentHashMap<S, Boolean>());
                    set.add(clientSession);
                    Set oldSet = deviceNotificationMap.putIfAbsent(dev, set);
                    if (oldSet != set) {
                        oldSet = oldSet != null ? oldSet : Collections.newSetFromMap(new ConcurrentHashMap<S, Boolean>());
                        oldSet.add(clientSession);
                    }
                }
            }
        }
    }

    public void unsubscribe(S clientSession, UUID... devices) {
        synchronized (clientSession) {
            for (UUID dev : devices) {
                synchronized (deviceNotificationMap) {
                    Set set = deviceNotificationMap.get(dev);
                    if (set != null) {
                        set.remove(clientSession);
                        if (set.isEmpty()) {
                            deviceNotificationMap.remove(dev);
                        }
                    }
                }
            }
        }
    }


    public Set<S> getSubscriptions(UUID device) {
        return deviceNotificationMap.get(device);
    }
}

package com.devicehive.websockets.subscriptions;


import javax.inject.Singleton;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;



abstract class SimpleSubscriptionsManager<S> implements SubscriptionsManager<S> {

    private ConcurrentMap<Long, Set<S>> deviceNotificationMap = new ConcurrentHashMap<Long, Set<S>>();



    public SimpleSubscriptionsManager() {
    }


    public void subscribe(S clientSession, long... devices) {
        synchronized (clientSession) {
            for (Long dev : devices) {
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

    public void unsubscribe(S clientSession, long... devices) {
        synchronized (clientSession) {
            for (Long dev : devices) {
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


    public Set<S> getSubscriptions(long device) {
        return deviceNotificationMap.get(device);
    }
}

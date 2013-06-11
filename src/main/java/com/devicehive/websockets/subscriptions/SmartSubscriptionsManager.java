package com.devicehive.websockets.subscriptions;


import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


abstract class SmartSubscriptionsManager<S> implements SubscriptionsManager<S> {

    private ConcurrentMap<Long, Set<S>> deviceNotificationMap = new ConcurrentHashMap<Long, Set<S>>();



    public SmartSubscriptionsManager() {
    }


    public void subscribe(S clientSession, long... devices) {
        synchronized (clientSession) {
            for (Long dev : devices) {

                boolean added = false;

                while (! added) {
                    Set<S> set = Collections.newSetFromMap(new ConcurrentHashMap<S, Boolean>());
                    set.add(clientSession);
                    Set oldSet = deviceNotificationMap.putIfAbsent(dev, set);
                    if (oldSet != null) {
                        synchronized (oldSet) {
                            if (!oldSet.isEmpty()) {
                                oldSet.add(clientSession);
                                added = true;
                            }
                        }
                    } else {
                        added = true;
                    }
                    if (!added) {
                        System.out.println("retry");
                    }
                }
            }
        }
    }

    public void unsubscribe(S clientSession, long... devices) {
        synchronized (clientSession) {
            for (Long dev : devices) {
                Set set = deviceNotificationMap.get(dev);
                if (set != null) {
                    synchronized (set) {
                        set.remove(clientSession);
                        deviceNotificationMap.remove(dev, Collections.emptySet());
                    }
                }
            }
        }
    }


    public Set<S> getSubscriptions(long device) {
        return deviceNotificationMap.get(device);
    }
}

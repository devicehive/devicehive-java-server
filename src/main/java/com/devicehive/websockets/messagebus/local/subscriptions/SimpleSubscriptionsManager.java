package com.devicehive.websockets.messagebus.local.subscriptions;


import javax.websocket.Session;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;



abstract class SimpleSubscriptionsManager extends SubscriptionsManager {

    private ConcurrentMap<UUID, Set<Session>> deviceNotificationMap = new ConcurrentHashMap<UUID, Set<Session>>();



    public SimpleSubscriptionsManager() {
    }


    public void subscribe(Session clientSession, UUID... devices) {
        synchronized (clientSession) {
            for (UUID dev : devices) {
                synchronized (deviceNotificationMap) {
                    Set<Session> set = Collections.newSetFromMap(new ConcurrentHashMap<Session, Boolean>());
                    set.add(clientSession);
                    Set oldSet = deviceNotificationMap.putIfAbsent(dev, set);
                    if (oldSet != set) {
                        oldSet = oldSet != null ? oldSet : Collections.newSetFromMap(new ConcurrentHashMap<Session, Boolean>());
                        oldSet.add(clientSession);
                    }
                }
            }
            if (!clientSession.getUserProperties().containsKey(SUBSCRIBED_FOR_NOTIFICATIONS_DEVICE_UUIDS)) {
                clientSession.getUserProperties().put(SUBSCRIBED_FOR_NOTIFICATIONS_DEVICE_UUIDS, Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>()));
            }
            Set<UUID> set = (Set<UUID>) clientSession.getUserProperties().get(SUBSCRIBED_FOR_NOTIFICATIONS_DEVICE_UUIDS);
            set.addAll(Arrays.asList(devices));
        }
    }

    public void unsubscribe(Session clientSession, UUID... devices) {
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
            if (!clientSession.getUserProperties().containsKey(SUBSCRIBED_FOR_NOTIFICATIONS_DEVICE_UUIDS)) {
                clientSession.getUserProperties().put(SUBSCRIBED_FOR_NOTIFICATIONS_DEVICE_UUIDS, Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>()));
            }
            Set<UUID> set = (Set<UUID>) clientSession.getUserProperties().get(SUBSCRIBED_FOR_NOTIFICATIONS_DEVICE_UUIDS);
            set.removeAll(Arrays.asList(devices));
        }
    }


    public Set<Session> getSubscriptions(UUID device) {
        return deviceNotificationMap.get(device);
    }
}

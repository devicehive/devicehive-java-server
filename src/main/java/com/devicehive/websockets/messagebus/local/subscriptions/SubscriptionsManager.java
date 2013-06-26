package com.devicehive.websockets.messagebus.local.subscriptions;

import javax.websocket.Session;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;


abstract class SubscriptionsManager {
    public abstract void subscribe(Session clientSession, Collection<UUID> devices);

    public abstract void unsubscribe(Session clientSession, Collection<UUID> devices);


    public abstract Set<Session> getSubscriptions(UUID device);


    public void unsubscribe(Session clientSession) {
        synchronized (clientSession) {
            if (clientSession.getUserProperties().containsKey(SUBSCRIBED_FOR_NOTIFICATIONS_DEVICE_UUIDS)) {
                Set<UUID> set = (Set<UUID>) clientSession.getUserProperties().get(SUBSCRIBED_FOR_NOTIFICATIONS_DEVICE_UUIDS);
                unsubscribe(clientSession, set);
            }
        }
    }


    public static final String SUBSCRIBED_FOR_NOTIFICATIONS_DEVICE_UUIDS = "SUBSCRIBED_FOR_NOTIFICATIONS_DEVICE_UUIDS";

}

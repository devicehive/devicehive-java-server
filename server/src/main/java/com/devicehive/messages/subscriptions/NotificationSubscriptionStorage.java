package com.devicehive.messages.subscriptions;

import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebsocketSession;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@Singleton
@ConcurrencyManagement(BEAN)
public class NotificationSubscriptionStorage extends AbstractStorage<Long, NotificationSubscription> {

    @EJB
    private SessionMonitor sessionMonitor;

    public Set<NotificationSubscription> getByDeviceId(Long id) {
        return get(id);
    }

    @Deprecated
    public Set<NotificationSubscription> getBySession(String sessionId) {
        Set<UUID> subs = WebsocketSession.getNotificationSubscriptions(sessionMonitor.getSession(sessionId));
        Set<NotificationSubscription> result = new HashSet<>();
        for (UUID sub : subs) {
            result.addAll(get(sub.toString()));
        }
        return result;
    }

    public Set<NotificationSubscription> getBySubscriptionId(UUID subscriptionId) {
        return get(subscriptionId.toString());
    }

    public synchronized void removeBySession(String sessionId) {
        Set<UUID> subs = WebsocketSession.getNotificationSubscriptions(sessionMonitor.getSession(sessionId));
        for (UUID sub : subs) {
            removeBySubscriptionId(sub.toString());
        }
    }

    public synchronized void removeByDevice(Long deviceId) {
        removeByEventSource(deviceId);
    }

}

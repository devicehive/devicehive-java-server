package com.devicehive.websockets.messagebus.local.subscriptions;


import javax.websocket.Session;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NotificationsSubscriptionManager {

    private final Lock lock = new ReentrantLock();

    private final ConcurrentMap<UUID, Set<Session>> notificationsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Session, Set<UUID>> notificationsReverseMap = new ConcurrentHashMap<>();


    private final Set<Session> allNotificationsSet = Collections.newSetFromMap(new ConcurrentHashMap<Session, Boolean>());

    public void subscribeForDeviceNotifications(Session session, Collection<UUID> devices) {
        try {
            lock.lock();
            for (UUID device : devices) {
                notificationsMap.putIfAbsent(device, Collections.newSetFromMap(new ConcurrentHashMap<Session, Boolean>()));
                notificationsMap.get(device).add(session);
            }
            notificationsReverseMap.putIfAbsent(session, Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>()));
            notificationsReverseMap.get(session).addAll(devices);
        } finally {
            lock.unlock();
        }
    }


    public void subscribeForDeviceNotifications(Session session) {
        try {
            lock.lock();
            allNotificationsSet.add(session);
        } finally {
            lock.unlock();
        }
    }


    public void unsubscribeFromDeviceNotifications(Session session, Collection<UUID> devices) {
        try {
            lock.lock();
            for (UUID device : devices) {
                Set<Session> deviceSessions = notificationsMap.get(device);
                if (deviceSessions != null) {
                    deviceSessions.remove(session);
                    if (deviceSessions.isEmpty()) {
                        notificationsMap.remove(device);
                    }
                }
            }
            Set<UUID> sessionDevices = notificationsReverseMap.get(session);
            if (sessionDevices != null) {
                sessionDevices.removeAll(devices);
                if (sessionDevices.isEmpty()) {
                    notificationsReverseMap.remove(session);
                }
            }
        } finally {
            lock.unlock();
        }
    }


    public void unsubscribeFromDeviceNotifications(Session session) {
        try {
            lock.lock();
            allNotificationsSet.remove(session);
            Set<UUID> sessionDevices = notificationsReverseMap.remove(session);
            if (sessionDevices != null) {
                for (UUID device : sessionDevices) {
                    Set<Session> deviceSessions = notificationsMap.get(device);
                    if (deviceSessions != null) {
                        deviceSessions.remove(session);
                        if (deviceSessions.isEmpty()) {
                            notificationsMap.remove(device);
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public Set<Session> getSubscriptions(UUID uuid) {
        return notificationsMap.get(uuid);
    }

    public Set<Session> getSubscribedForAll() {
        return allNotificationsSet;
    }
}

package com.devicehive.websockets;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.websockets.util.HiveEndpoint;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HiveWebSocketSessionState {

    public static final String KEY = HiveWebSocketSessionState.class.getName();
    private final Lock queueLock = new ReentrantLock(true);
    private final ConcurrentLinkedQueue<JsonElement> queue = new ConcurrentLinkedQueue<>();
    private final Set<String> commandSubscriptions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> commandUpdateSubscriptions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Lock commandSubscriptionsLock = new ReentrantLock(true);
    private final Set<String> notificationSubscriptions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Lock notificationSubscriptionsLock = new ReentrantLock(true);
    private final Lock commandUpdateSubscriptionsLock = new ReentrantLock(true);
    private final ConcurrentMap<Set<String>, Set<String>> oldFormatCommandSubscriptions = Maps.newConcurrentMap();
    private final ConcurrentMap<Set<String>, Set<String>> oldFormatNotificationSubscriptions = Maps.newConcurrentMap();
    private HiveEndpoint endpoint;
    private HivePrincipal hivePrincipal;

    public static HiveWebSocketSessionState get(WebSocketSession session) {
        return (HiveWebSocketSessionState) session.getAttributes().get(HiveWebSocketSessionState.KEY);
    }

    public HiveEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(HiveEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Lock getQueueLock() {
        return queueLock;
    }

    public ConcurrentLinkedQueue<JsonElement> getQueue() {
        return queue;
    }

    public Set<String> getCommandSubscriptions() {
        return commandSubscriptions;
    }

    public Set<String> getCommandUpdateSubscriptions() {
        return commandUpdateSubscriptions;
    }

    public Lock getCommandSubscriptionsLock() {
        return commandSubscriptionsLock;
    }

    public Set<String> getNotificationSubscriptions() {
        return notificationSubscriptions;
    }

    public Lock getNotificationSubscriptionsLock() {
        return notificationSubscriptionsLock;
    }

    public HivePrincipal getHivePrincipal() {
        return hivePrincipal;
    }

    public void setHivePrincipal(HivePrincipal hivePrincipal) {
        this.hivePrincipal = hivePrincipal;
    }

    public Lock getCommandUpdateSubscriptionsLock() {
        return commandUpdateSubscriptionsLock;
    }

    public synchronized void addOldFormatCommandSubscription(Set<String> guids, String subscriptionId) {
        Set<String> toStore = guids == null
                ? Sets.newHashSet(Constants.NULL_SUBSTITUTE)
                : guids;

        if (oldFormatCommandSubscriptions.containsKey(toStore)) {
            Set<String> existingSubscriptions = oldFormatCommandSubscriptions.get(toStore);
            existingSubscriptions.add(subscriptionId);
        } else {
            Set<String> subscriptions = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
            subscriptions.add(subscriptionId);
            oldFormatCommandSubscriptions.put(toStore, subscriptions);
        }
    }

    public synchronized Set<String> removeOldFormatCommandSubscription(Set<String> guids) {
        Set<String> toRemove = guids == null
                ? new HashSet<String>() {
            {
                add(Constants.NULL_SUBSTITUTE);
            }

            private static final long serialVersionUID = -8106785048967338278L;
        }
                : guids;
        return oldFormatCommandSubscriptions.remove(toRemove);
    }

    public synchronized void addOldFormatNotificationSubscription(Set<String> guids, String subscriptionId) {
        Set<String> toStore = guids == null
                ? Sets.newHashSet(Constants.NULL_SUBSTITUTE)
                : guids;

        if (oldFormatNotificationSubscriptions.containsKey(toStore)) {
            Set<String> existingSubscriptions = oldFormatNotificationSubscriptions.get(toStore);
            existingSubscriptions.add(subscriptionId);
        } else {
            Set<String> subscriptions = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
            subscriptions.add(subscriptionId);
            oldFormatNotificationSubscriptions.put(toStore, subscriptions);
        }
    }

    public synchronized Set<String> removeOldFormatNotificationSubscription(Set<String> guids) {
        Set<String> toRemove = guids == null
                ? new HashSet<String>() {
            {
                add(Constants.NULL_SUBSTITUTE);
            }

            private static final long serialVersionUID = 599925075379032426L;
        }
                : guids;
        return oldFormatNotificationSubscriptions.remove(toRemove);
    }
}

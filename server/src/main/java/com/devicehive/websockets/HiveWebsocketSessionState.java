package com.devicehive.websockets;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import javax.websocket.Session;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HiveWebsocketSessionState {

    public static final String KEY = HiveWebsocketSessionState.class.getName();

    private final Lock queueLock = new ReentrantLock(true);

    private final ConcurrentLinkedQueue<JsonElement> queue = new ConcurrentLinkedQueue<>();

    private final Set<UUID> commandSubscriptions = Sets.newConcurrentHashSet();

    private final Lock commandSubscriptionsLock = new ReentrantLock(true);

    private final Set<UUID> notificationSubscriptions = Sets.newConcurrentHashSet();

    private final Lock notificationSubscriptionsLock = new ReentrantLock(true);

    private final Lock commandUpdateSubscriptionsLock = new ReentrantLock(true);

    private final ConcurrentMap<Set<String>, Set<UUID>> oldFormatCommandSubscriptions = Maps.newConcurrentMap();

    private final ConcurrentMap<Set<String>, Set<UUID>> oldFormatNotificationSubscriptions = Maps.newConcurrentMap();

    private HivePrincipal hivePrincipal;


    public static HiveWebsocketSessionState get(Session session) {
        return (HiveWebsocketSessionState) session.getUserProperties().get(HiveWebsocketSessionState.KEY);
    }


    public Lock getQueueLock() {
        return queueLock;
    }

    public ConcurrentLinkedQueue<JsonElement> getQueue() {
        return queue;
    }

    public Set<UUID> getCommandSubscriptions() {
        return commandSubscriptions;
    }

    public Lock getCommandSubscriptionsLock() {
        return commandSubscriptionsLock;
    }

    public Set<UUID> getNotificationSubscriptions() {
        return notificationSubscriptions;
    }

    public Lock getNotificationSubscriptionsLock() {
        return notificationSubscriptionsLock;
    }

    public ConcurrentMap<Set<String>, Set<UUID>> getOldFormatCommandSubscriptions() {
        return oldFormatCommandSubscriptions;
    }

    public ConcurrentMap<Set<String>, Set<UUID>> getOldFormatNotificationSubscriptions() {
        return oldFormatNotificationSubscriptions;
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

    public synchronized void addOldFormatCommandSubscription(Set<String> guids, UUID subscriptionId) {
        Set<String> toStore = guids == null
                ? Sets.newHashSet(Constants.NULL_SUBSTITUTE)
                : guids;

        if (oldFormatCommandSubscriptions.containsKey(toStore)) {
            Set<UUID> existingSubscriptions = oldFormatCommandSubscriptions.get(toStore);
            existingSubscriptions.add(subscriptionId);
        } else {
            Set<UUID> subscriptions = Sets.newConcurrentHashSet();
            subscriptions.add(subscriptionId);
            oldFormatCommandSubscriptions.put(toStore, subscriptions);
        }
    }

    public synchronized Set<UUID> removeOldFormatCommandSubscription(Set<String> guids) {
        Set<String> toRemove = guids == null
                ? new HashSet<String>() {{
            add(Constants.NULL_SUBSTITUTE);
        }}
                : guids;
        return oldFormatCommandSubscriptions.remove(toRemove);
    }

    public synchronized void addOldFormatNotificationSubscription(Set<String> guids, UUID subscriptionId) {
        Set<String> toStore = guids == null
                ? Sets.newHashSet(Constants.NULL_SUBSTITUTE)
                : guids;

        if (oldFormatNotificationSubscriptions.containsKey(toStore)) {
            Set<UUID> existingSubscriptions = oldFormatNotificationSubscriptions.get(toStore);
            existingSubscriptions.add(subscriptionId);
        } else {
            Set<UUID> subscriptions = Sets.newConcurrentHashSet();
            subscriptions.add(subscriptionId);
            oldFormatNotificationSubscriptions.put(toStore, subscriptions);
        }
    }

    public synchronized Set<UUID> removeOldFormatNotificationSubscription(Set<String> guids) {
        Set<String> toRemove = guids == null
                ? new HashSet<String>() {{
            add(Constants.NULL_SUBSTITUTE);
        }}
                : guids;
        return oldFormatNotificationSubscriptions.remove(toRemove);
    }
}

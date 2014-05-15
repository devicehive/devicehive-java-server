package com.devicehive.websockets.util;


import com.devicehive.auth.HivePrincipal;
import com.google.gson.JsonElement;
import org.eclipse.jetty.util.ConcurrentHashSet;

import javax.websocket.Session;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WebsocketSession {
    public static final String COMMANDS_SUBSCRIPTIONS = "COMMANDS_SUBSCRIPTIONS";
    public static final String COMMANDS_SUBSCRIPTION_LOCK = "COMMANDS_SUBSCRIPTION_LOCK";
    public static final String COMMAND_UPDATES_SUBSCRIPTION_LOCK = "COMMAND_UPDATES_SUBSCRIPTION_LOCK";
    public static final String NOTIFICATION_SUBSCRIPTION_LOCK = "NOTIFICATION_SUBSCRIPTION_LOCK";
    public static final String NOTIFICATIONS_SUBSCRIPTIONS = "NOTIFICATIONS_SUBSCRIPTIONS";
    public static final String OLD_FORMAT_COMMAND_SUBSCRIPTIONS = "OLD_FORMAT_COMMAND_SUBSCRIPTIONS";
    public static final String OLD_FORMAT_NOTIFICATION_SUBSCRIPTIONS = "OLD_FORMAT_NOTIFICATION_SUBSCRIPTIONS";
    public static final String QUEUE = "QUEUE";
    private static final String PRINCIPAL = "HIVE_PRINCIPAL";
    private static final String QUEUE_LOCK = "QUEUE_LOCK";

    public static Lock getQueueLock(Session session) {
        return (Lock) session.getUserProperties().get(QUEUE_LOCK);
    }

    public static HivePrincipal getPrincipal(Session session) {
        return (HivePrincipal) session.getUserProperties().get(PRINCIPAL);
    }

    public static void setPrincipal(Session session, HivePrincipal principal) {
        session.getUserProperties().put(PRINCIPAL, principal);
    }

    public static Lock getCommandsSubscriptionsLock(Session session) {
        return (Lock) session.getUserProperties().get(COMMANDS_SUBSCRIPTION_LOCK);
    }

    public static void createCommandsSubscriptionsLock(Session session) {
        if (!session.getUserProperties().containsKey(COMMANDS_SUBSCRIPTION_LOCK)) {
            session.getUserProperties().put(COMMANDS_SUBSCRIPTION_LOCK, new ReentrantLock(true));
        }
    }

    public static Lock getCommandUpdatesSubscriptionsLock(Session session) {
        return (Lock) session.getUserProperties().get(COMMAND_UPDATES_SUBSCRIPTION_LOCK);
    }

    public static void createCommandUpdatesSubscriptionsLock(Session session) {
        if (!session.getUserProperties().containsKey(COMMAND_UPDATES_SUBSCRIPTION_LOCK)) {
            session.getUserProperties().put(COMMAND_UPDATES_SUBSCRIPTION_LOCK, new ReentrantLock(true));
        }
    }

    public static Lock getNotificationSubscriptionsLock(Session session) {
        return (Lock) session.getUserProperties().get(NOTIFICATION_SUBSCRIPTION_LOCK);
    }

    public static void createNotificationSubscriptionsLock(Session session) {
        if (!session.getUserProperties().containsKey(NOTIFICATION_SUBSCRIPTION_LOCK)) {
            session.getUserProperties().put(NOTIFICATION_SUBSCRIPTION_LOCK, new ReentrantLock(true));
        }
    }

    public static void createQueueLock(Session session) {
        if (!session.getUserProperties().containsKey(QUEUE)) {
            session.getUserProperties().put(QUEUE, new ConcurrentLinkedQueue<JsonElement>());
        }
        if (!session.getUserProperties().containsKey(QUEUE_LOCK)) {
            session.getUserProperties().put(QUEUE_LOCK, new ReentrantLock(true));
        }
    }

    public static Set<UUID> getCommandSubscriptions(Session session) {
        return (Set<UUID>) session.getUserProperties().get(COMMANDS_SUBSCRIPTIONS);
    }

    public static Set<UUID> getNotificationSubscriptions(Session session) {
        return (Set<UUID>) session.getUserProperties().get(NOTIFICATIONS_SUBSCRIPTIONS);
    }

    public static void addOldFormatCommandSubscription(Session session, Set<String> guids, UUID subscriptionId) {
        addOldFormatSubscription(session, guids, subscriptionId, OLD_FORMAT_COMMAND_SUBSCRIPTIONS);
    }

    public static void addOldFormatNotificationSubscription(Session session, Set<String> guids, UUID subscriptionId) {
        addOldFormatSubscription(session, guids, subscriptionId, OLD_FORMAT_NOTIFICATION_SUBSCRIPTIONS);
    }

    private static Set<UUID> removeOldFormatSubscription(Session session, Set<String> guids, String storageName) {
        Map<Set<String>, Set<UUID>> oldFormatSubscriptions =
                (Map<Set<String>, Set<UUID>>) session.getUserProperties().get(storageName);
        return oldFormatSubscriptions.remove(guids);
    }

    public static Set<UUID> removeOldFormatCommandSubscription(Session session, Set<String> guids) {
        return removeOldFormatSubscription(session, guids, OLD_FORMAT_COMMAND_SUBSCRIPTIONS);
    }

    public static Set<UUID> removeOldFormatNotificationSubscription(Session session, Set<String> guids) {
        return removeOldFormatSubscription(session, guids, OLD_FORMAT_NOTIFICATION_SUBSCRIPTIONS);
    }

    private static void addOldFormatSubscription(Session session, Set<String> guids, UUID subscriptionId,
                                                 String storageName) {
        Map<Set<String>, Set<UUID>> oldFormatSubscriptions =
                (Map<Set<String>, Set<UUID>>) session.getUserProperties().get(storageName);
        if (oldFormatSubscriptions.containsKey(guids)) {
            Set<UUID> existingSubscriptions = oldFormatSubscriptions.get(guids);
            existingSubscriptions.add(subscriptionId);
            oldFormatSubscriptions.put(guids, existingSubscriptions);
        } else {
            Set<UUID> subscriptions = new HashSet<>();
            subscriptions.add(subscriptionId);
            oldFormatSubscriptions.put(guids, subscriptions);
        }
    }

    public static void createSubscriptions(Session session) {
        Map<String, Object> userProperties = session.getUserProperties();
        userProperties.put(COMMANDS_SUBSCRIPTIONS, new ConcurrentHashSet<UUID>());
        userProperties.put(NOTIFICATIONS_SUBSCRIPTIONS, new ConcurrentHashSet<UUID>());
        userProperties.put(OLD_FORMAT_COMMAND_SUBSCRIPTIONS, new ConcurrentHashMap<Set<String>, Set<UUID>>());
        userProperties.put(OLD_FORMAT_NOTIFICATION_SUBSCRIPTIONS, new ConcurrentHashMap<Set<String>, Set<UUID>>());
    }

    public static void addMessagesToQueue(Session session, JsonElement... jsons) {
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<JsonElement> queue = (ConcurrentLinkedQueue) session.getUserProperties().get(QUEUE);
        Collections.addAll(queue, jsons);
    }
}



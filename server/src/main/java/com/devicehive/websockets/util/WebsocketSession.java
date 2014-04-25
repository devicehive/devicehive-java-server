package com.devicehive.websockets.util;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.google.gson.JsonElement;

import javax.websocket.Session;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WebsocketSession {
    private static final String PRINCIPAL = "HIVE_PRINCIPAL";
    public static final String COMMANDS_SUBSCRIPTIONS = "COMMANDS_SUBSCRIPTIONS";
    public static final String COMMANDS_SUBSCRIPTION_LOCK = "COMMANDS_SUBSCRIPTION_LOCK";
    public static final String COMMAND_UPDATES_SUBSCRIPTION_LOCK = "COMMAND_UPDATES_SUBSCRIPTION_LOCK";
    public static final String NOTIFICATIONS_LOCK = "NOTIFICATIONS_LOCK";
    public static final String NOTIFICATIONS_SUBSCRIPTIONS = "NOTIFICATIONS_SUBSCRIPTIONS";
    private static final String QUEUE_LOCK = "QUEUE_LOCK";
    public static final String QUEUE = "QUEUE";

    public static Lock getQueueLock(Session session) {
        return (Lock) session.getUserProperties().get(QUEUE_LOCK);
    }

    public static HivePrincipal getPrincipal(Session session){
        return  (HivePrincipal) session.getUserProperties().get(PRINCIPAL);
    }

    public static void setPrincipal(Session session, HivePrincipal principal){
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

    public static void createCommandUpdatesSubscriptionsLock(Session session) {
        if (!session.getUserProperties().containsKey(COMMAND_UPDATES_SUBSCRIPTION_LOCK)) {
            session.getUserProperties().put(COMMAND_UPDATES_SUBSCRIPTION_LOCK, new ReentrantLock(true));
        }
    }

    public static Lock getNotificationSubscriptionsLock(Session session) {
        return (Lock) session.getUserProperties().get(NOTIFICATIONS_LOCK);
    }

    public static void createNotificationSubscriptionsLock(Session session) {
        if (!session.getUserProperties().containsKey(NOTIFICATIONS_LOCK)) {
            session.getUserProperties().put(NOTIFICATIONS_LOCK, new ReentrantLock(true));
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

    public static List<CommandSubscription> getCommandSubscriptions(Session session) {
        return (List<CommandSubscription>) session.getUserProperties().get(COMMANDS_SUBSCRIPTIONS);
    }

    public static void setCommandSubscriptions(Session session, List<CommandSubscription> subs) {
        session.getUserProperties().put(COMMANDS_SUBSCRIPTIONS, subs);
    }

    public static List<CommandSubscription> removeCommandSubscriptions(Session session) {
        return (List<CommandSubscription>) session.getUserProperties().remove(COMMANDS_SUBSCRIPTIONS)  ;
    }

    public static List<NotificationSubscription> getNotificationSubscriptions(Session session) {
        return (List<NotificationSubscription>) session.getUserProperties().get(NOTIFICATIONS_SUBSCRIPTIONS);
    }

    public static void setNotificationSubscriptions(Session session, List<NotificationSubscription> subs) {
        session.getUserProperties().put(NOTIFICATIONS_SUBSCRIPTIONS, subs);
    }

    public static List<NotificationSubscription>  removeNotificationSubscriptions(Session session) {
        return (List<NotificationSubscription>) session.getUserProperties().remove(NOTIFICATIONS_SUBSCRIPTIONS);
    }


    public static void addMessagesToQueue(Session session, JsonElement... jsons) {
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<JsonElement> queue = (ConcurrentLinkedQueue) session.getUserProperties().get(QUEUE);
        for (JsonElement json : jsons) {
            queue.add(json);
        }
    }
}



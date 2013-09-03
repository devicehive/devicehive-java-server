package com.devicehive.websockets.util;


import com.devicehive.model.domain.Device;
import com.devicehive.model.domain.User;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WebsocketSession {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketSession.class);
    private static final String AUTHORISED_USER = "AUTHORISED_USER";
    private static final String AUTHORISED_DEVICE = "AUTHORISED_DEVICE";
    public static final String COMMANDS_SUBSCRIPTION_LOCK = "COMMANDS_SUBSCRIPTION_LOCK";
    public static final String COMMAND_UPDATES_SUBSCRIPTION_LOCK = "COMMAND_UPDATES_SUBSCRIPTION_LOCK";
    public static final String NOTIFICATIONS_LOCK = "NOTIFICATIONS_LOCK";
    private static final String QUEUE_LOCK = "QUEUE_LOCK";
    public static final String QUEUE = "QUEUE";

    public static Lock getQueueLock(Session session) {
        return (Lock) session.getUserProperties().get(QUEUE_LOCK);
    }

    public static User getAuthorisedUser(Session session) {
        return (User) session.getUserProperties().get(AUTHORISED_USER);

    }

    public static boolean hasAuthorisedUser(Session session) {
        return getAuthorisedUser(session) != null;
    }

    public static void setAuthorisedUser(Session session, User user) {
        session.getUserProperties().put(AUTHORISED_USER, user);
    }

    public static Device getAuthorisedDevice(Session session) {
        return (Device) session.getUserProperties().get(AUTHORISED_DEVICE);
    }

    public static void setAuthorisedDevice(Session session, Device device) {
        session.getUserProperties().put(AUTHORISED_DEVICE, device);
    }

    public static boolean hasAuthorisedDevice(Session session) {
        return getAuthorisedDevice(session) != null;
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


    public static void addMessagesToQueue(Session session, JsonElement... jsons) {
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<JsonElement> queue = (ConcurrentLinkedQueue) session.getUserProperties().get(QUEUE);
        for (JsonElement json : jsons) {
            queue.add(json);
        }
    }
}



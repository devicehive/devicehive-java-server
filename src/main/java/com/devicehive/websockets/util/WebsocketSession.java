package com.devicehive.websockets.util;


import com.devicehive.model.Device;
import com.devicehive.model.User;
import com.devicehive.websockets.handlers.ClientMessageHandlers;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WebsocketSession {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketSession.class);

    private static final String AUTHORISED_USER = "AUTHORISED_USER";
    private static final String AUTHORISED_DEVICE = "AUTHORISED_DEVICE";
    private static final String WEAK_AUTHORISED_DEVICE = "WEAK_AUTHORISED_DEVICE";
    private static final String COMMANDS_SUBSCRIPTION_LOCK = "COMMANDS_SUBSCRIPTION_LOCK";
    private static final String COMMAND_UPDATES_SUBSCRIPTION_LOCK = "COMMAND_UPDATES_SUBSCRIPTION_LOCK";

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
            session.getUserProperties().put(COMMANDS_SUBSCRIPTION_LOCK, new ReentrantLock());
        }
    }


    public static Lock getCommandUpdatesSubscriptionsLock(Session session) {
        return (Lock) session.getUserProperties().get(COMMAND_UPDATES_SUBSCRIPTION_LOCK);
    }

    public static void createCommandUpdatesSubscriptionsLock(Session session) {
        if (!session.getUserProperties().containsKey(COMMAND_UPDATES_SUBSCRIPTION_LOCK)) {
            session.getUserProperties().put(COMMAND_UPDATES_SUBSCRIPTION_LOCK, new ReentrantLock());
        }
    }



    public static void deliverMessages(Session session, JsonObject... jsonObjects) {
        synchronized (session) {
            for (final JsonObject jsonObject : jsonObjects) {
                session.getAsyncRemote().sendText(jsonObject.toString(), new SendHandler() {
                    @Override
                    public void onResult(SendResult result) {
                        if (!result.isOK()) {
                            logger.error("Error message delivery", result.getException());
                        }
                    }
                });
            }
        }
    }
}

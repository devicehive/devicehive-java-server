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

public class WebsocketSession {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketSession.class);

    private static final String AUTHENTICATED_USER = "AUTHENTICATED_USER";

    private static final String AUTHENTICATED_DEVICE = "AUTHENTICATED_DEVICE";

    public static User getAuthorisedUser(Session session) {
        return (User) session.getUserProperties().get(AUTHENTICATED_USER);
    }

    public static boolean hasAuthorisedUser(Session session) {
        return getAuthorisedUser(session) != null;
    }

    public static void setAuthorisedUser(Session session, User user) {
        session.getUserProperties().put(AUTHENTICATED_USER, user);
    }

    public static Device getAuthorisedDevice(Session session) {
        return (Device) session.getUserProperties().get(AUTHENTICATED_DEVICE);
    }

    public static void setAuthorisedDevice(Session session, Device device) {
        session.getUserProperties().put(AUTHENTICATED_DEVICE, device);
    }

    public static boolean hasAuthorisedDevice(Session session) {
        return getAuthorisedDevice(session) != null;
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

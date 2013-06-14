package com.devicehive.websockets.util;

import com.devicehive.model.DeviceCommand;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 13.06.13
 * Time: 17:20
 * To change this template use File | Settings | File Templates.
 */
public class WebsocketUtil {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketUtil.class);

    public static boolean sendMessage(JsonObject jsonObject, Session session) {
        String message = jsonObject.toString();
        try {
            synchronized (session) {
                session.getBasicRemote().sendText(jsonObject.toString());
            }
            return true;
        } catch (IOException ex) {
            logger.error("Error delivering message " + message, ex);
            return false;
        }
    }

    public static void sendMessageAsync(JsonObject jsonObject, Session session) {
        final String message = jsonObject.toString();
        synchronized (session) {
            session.getAsyncRemote().sendText(message, new SendHandler() {
            public void onResult(SendResult result) {
                if (!result.isOK()) {
                    logger.error("Error delivering message " + message, result.getException());
                }
                }
        });
        }
    }
}

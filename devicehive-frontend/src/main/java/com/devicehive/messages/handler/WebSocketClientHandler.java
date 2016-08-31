package com.devicehive.messages.handler;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

public class WebSocketClientHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientHandler.class);

    public static void sendMessage(JsonObject json, WebSocketSession session) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(json.toString()));
        } catch (IOException e) {
            logger.error("Exception while sending message", e);
        }
    }
}

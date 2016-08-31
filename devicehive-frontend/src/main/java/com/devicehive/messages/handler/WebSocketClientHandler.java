package com.devicehive.messages.handler;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

public class WebSocketClientHandler implements ClientHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientHandler.class);

    private WebSocketSession session;


    public WebSocketClientHandler(WebSocketSession session) {
        this.session = session;
    }

    @Override
    public void sendMessage(JsonObject json) {
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

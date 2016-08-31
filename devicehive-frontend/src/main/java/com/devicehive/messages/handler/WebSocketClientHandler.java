package com.devicehive.messages.handler;

import com.devicehive.websockets.util.WSMessageSupplier;
import com.google.gson.JsonObject;
import org.springframework.web.socket.WebSocketSession;

public class WebSocketClientHandler implements ClientHandler {

    private WebSocketSession session;
    private WSMessageSupplier messageSupplier;


    public WebSocketClientHandler(WebSocketSession session, WSMessageSupplier messageSupplier) {
        this.session = session;
        this.messageSupplier = messageSupplier;
    }

    @Override
    public void sendMessage(JsonObject json) {
        if (!session.isOpen()) {
            return;
        }
        messageSupplier.deliver(json, session);
    }
}

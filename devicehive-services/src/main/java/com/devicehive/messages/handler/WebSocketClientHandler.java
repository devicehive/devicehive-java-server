package com.devicehive.messages.handler;

import com.devicehive.websockets.HiveWebsocketSessionState;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.google.gson.JsonObject;
import org.springframework.web.socket.WebSocketSession;

public class WebSocketClientHandler implements ClientHandler {

    private WebSocketSession session;
    private AsyncMessageSupplier messageSupplier;

    public WebSocketClientHandler(WebSocketSession session, AsyncMessageSupplier messageSupplier) {
        this.session = session;
        this.messageSupplier = messageSupplier;
    }

    @Override
    public void sendMessage(JsonObject json) {
        if (!session.isOpen()) {
            return;
        }
        HiveWebsocketSessionState.get(session).getQueue().add(json);
        messageSupplier.deliverMessages(session);
    }
}

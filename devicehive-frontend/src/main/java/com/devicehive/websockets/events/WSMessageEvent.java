package com.devicehive.websockets.events;

import com.google.gson.JsonElement;
import org.springframework.web.socket.WebSocketSession;

public class WSMessageEvent {

    private JsonElement message;
    private WebSocketSession session;

    public JsonElement getMessage() {
        return message;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public void setMessage(JsonElement message) {
        this.message = message;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }
}

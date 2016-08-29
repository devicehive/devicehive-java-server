package com.devicehive.websockets.events;

import com.google.gson.JsonObject;
import org.springframework.web.socket.WebSocketSession;

public class WSMessageEvent {

    private JsonObject request;
    private WebSocketSession session;

    public JsonObject getRequest() {
        return request;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public void setRequest(JsonObject request) {
        this.request = request;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }
}

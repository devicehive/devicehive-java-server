package com.devicehive.util;


import com.google.gson.JsonObject;
import org.springframework.web.socket.WebSocketSession;

public final class ThreadLocalVariablesKeeper {

    private static ThreadLocal<JsonObject> REQUEST = new ThreadLocal<>();
    private static ThreadLocal<WebSocketSession> SESSION = new ThreadLocal<>();

    public static JsonObject getRequest() {
        return REQUEST.get();
    }

    public static void setRequest(JsonObject request) {
        REQUEST.set(request);
    }

    public static WebSocketSession getSession() {
        return SESSION.get();
    }

    public static void setSession(WebSocketSession session) {
        SESSION.set(session);
    }


    public static void clean() {
        REQUEST.set(null);
        SESSION.set(null);
    }

}

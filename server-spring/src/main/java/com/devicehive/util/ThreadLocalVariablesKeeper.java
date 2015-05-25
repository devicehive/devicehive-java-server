package com.devicehive.util;


import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;

public final class ThreadLocalVariablesKeeper {

    private static Logger logger = LoggerFactory.getLogger(ThreadLocalVariablesKeeper.class);
    private static ThreadLocal<JsonObject> REQUEST = new ThreadLocal<>();
    private static ThreadLocal<Session> SESSION = new ThreadLocal<>();

    public static JsonObject getRequest() {
        return REQUEST.get();
    }

    public static void setRequest(JsonObject request) {
        REQUEST.set(request);
    }

    public static Session getSession() {
        return SESSION.get();
    }

    public static void setSession(Session session) {
        SESSION.set(session);
    }


    public static void clean() {
        REQUEST.set(null);
        SESSION.set(null);
    }

}

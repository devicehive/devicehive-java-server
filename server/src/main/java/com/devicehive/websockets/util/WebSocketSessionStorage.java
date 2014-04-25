package com.devicehive.websockets.util;


import javax.websocket.Session;
import java.util.HashMap;

public class WebSocketSessionStorage {

    private static final HashMap<String, Session> activeSessionsStorage = new HashMap<>();

    private WebSocketSessionStorage() {
    }

    public static Session getSession(String sessionId) {
        return activeSessionsStorage.get(sessionId);
    }

    public static void addSession(Session session) {
        activeSessionsStorage.put(session.getId(), session);
    }

    public static void removeSession(String id) {
        activeSessionsStorage.remove(id);
    }
}

package com.devicehive.websockets.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SubscriptionSessionMap {

    @Autowired
    private SessionMonitor sessionMonitor;

    private ConcurrentMap<UUID, String> map = new ConcurrentHashMap<>();


    public void put(UUID subId, WebSocketSession session) {
        map.put(subId, session.getId());
    }

    public WebSocketSession get(UUID subId) {
        String sessionId = map.get(subId);
        if (sessionId != null) {
            return sessionMonitor.getSession(sessionId);
        }
        return null;
    }

    public void removeAll(Collection<UUID> uuids) {
        if (uuids != null) {
            for (UUID uuid : uuids) {
                map.remove(uuid);
            }
        }
    }
}

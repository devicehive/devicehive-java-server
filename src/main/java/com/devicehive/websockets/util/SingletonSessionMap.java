package com.devicehive.websockets.util;

import javax.inject.Singleton;
import javax.websocket.Session;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created with IntelliJ IDEA.
 * User: jkulagina
 * Date: 26.06.13
 * Time: 14:32
 */
@Singleton
public class SingletonSessionMap {

    private ConcurrentMap<String, Session> sessionConcurrentMap = new ConcurrentHashMap<>();

    public void addSession(Session session){
         sessionConcurrentMap.putIfAbsent(session.getId(), session);
    }

    public void deleteSession(String sessionId){
        sessionConcurrentMap.remove(sessionId);
    }

    public Session getSession(String sessionId){
        return sessionConcurrentMap.get(sessionId);
    }
}

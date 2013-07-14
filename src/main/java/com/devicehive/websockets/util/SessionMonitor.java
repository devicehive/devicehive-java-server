package com.devicehive.websockets.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class SessionMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SessionMonitor.class);


    private Map<String, Session> sessionMap;

    private static final long TIMEOUT = 2 * 60 * 1000; // 2 minutes


    public void registerSession(final Session session){
        session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
            @Override
            public void onMessage(PongMessage message) {
                logger.debug("Pong received for session " + session.getId());
                session.getUserProperties().put("ping", System.currentTimeMillis());
            }
        });
        session.getUserProperties().put("ping", System.currentTimeMillis());
        sessionMap.put(session.getId(), session);
    }


    public Session getSession(String sessionId) {
        Session session = sessionMap.get(sessionId);
        return session != null && session.isOpen() ? session : null;
    }


    @Schedule(hour="*", minute = "*", second = "*/30")
    public void ping() {
        for (Session session : sessionMap.values()) {
            if (session.isOpen()) {
                logger.debug("Pinging session " + session.getId());
                try {
                    session.getAsyncRemote().sendPing(ByteBuffer.wrap("devicehive-ping".getBytes()));
                } catch (IOException ex) {
                    logger.error("Error sending ping, closing the session", ex);
                    closePingPong(session);
                }
            } else {
                logger.debug("Session " + session.getId() + " is closed.");
                sessionMap.remove(session.getId());
            }
        }
    }

    @Schedule(hour="*", minute = "*", second = "*/30")
    public void monitor() {
        for (Session session : sessionMap.values()) {
            logger.debug("Checking session " + session.getId());
            if (session.isOpen()) {
                long timestamp = (Long)session.getUserProperties().get("ping");
                if (System.currentTimeMillis() - timestamp > TIMEOUT) {
                    closePingPong(session);
                }
            } else {
                logger.debug("Session " + session.getId() + " is closed.");
                sessionMap.remove(session.getId());
            }
        }
    }


    private void closePingPong(Session session) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "No pongs for a long time"));
        } catch (IOException ex) {
            logger.error("Error closing session", ex);
        }
    }

    @PostConstruct
    public void init() {
        sessionMap = new ConcurrentHashMap<>();
    }

    @PreDestroy
    public void closeAllSessions() {
        for (Session session : sessionMap.values()) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.SERVICE_RESTART, "Shutdown"));
            } catch (IOException ex) {
                logger.error("Error closing session", ex);
            }
        }
    }
}

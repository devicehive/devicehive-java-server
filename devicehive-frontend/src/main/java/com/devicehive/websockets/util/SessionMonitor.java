package com.devicehive.websockets.util;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SessionMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SessionMonitor.class);

    private ConcurrentMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    public void registerSession(final WebSocketSession session) {
        sessionMap.put(session.getId(), session);
    }

    public WebSocketSession getSession(String sessionId) {
        WebSocketSession session = sessionMap.get(sessionId);
        return session != null && session.isOpen() ? session : null;
    }

    public void removeSession(String sessionId) throws IOException {
        sessionMap.remove(sessionId);
        WebSocketSession session = sessionMap.get(sessionId);
        try {
            if (session != null) session.close();
        } catch (IOException ex) {
            logger.error("Error closing session", ex);
        }
    }

    @Scheduled(cron = "0/30 * * * * *")
    public synchronized void ping() {
        try {
            for (WebSocketSession session : sessionMap.values()) {
                if (session.isOpen()) {
                    logger.debug("Pinging session {}", session.getId());
                    session.sendMessage(new PingMessage());
                } else {
                    logger.debug("Session {} is closed.", session.getId());
                    sessionMap.remove(session.getId());
                }
            }
        } catch (IOException e) {
            logger.error("Exception while ping session");
        }
    }

    @PreDestroy
    public void closeAllSessions() {
        for (WebSocketSession session : sessionMap.values()) {
            try {
                session.close(CloseStatus.SERVICE_RESTARTED);
            } catch (IOException ex) {
                logger.error("Error closing session", ex);
            }
        }
        sessionMap.clear();
    }
}

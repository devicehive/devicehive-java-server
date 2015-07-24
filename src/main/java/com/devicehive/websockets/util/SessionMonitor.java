package com.devicehive.websockets.util;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.Device;
import com.devicehive.service.DeviceActivityService;
import com.devicehive.websockets.HiveWebsocketSessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

@Component
public class SessionMonitor {
    private static final Logger logger = LoggerFactory.getLogger(SessionMonitor.class);

    private ConcurrentMap<String, WebSocketSession> sessionMap;

    @Autowired
    private DeviceActivityService deviceActivityService;
    @Autowired
    private SubscriptionManager subscriptionManager;

    public void registerSession(final WebSocketSession session) {
        sessionMap.put(session.getId(), session);
    }

    public WebSocketSession getSession(String sessionId) {
        WebSocketSession session = sessionMap.get(sessionId);
        return session != null && session.isOpen() ? session : null;
    }

    public void updateDeviceSession(WebSocketSession session) {
        HivePrincipal hivePrincipal = HiveWebsocketSessionState.get(session).getHivePrincipal();
        Device authorizedDevice = hivePrincipal != null ? hivePrincipal.getDevice() : null;
        if (authorizedDevice != null) {
            deviceActivityService.update(authorizedDevice.getGuid());
        }
        Set<UUID> commandSubscriptions = HiveWebsocketSessionState.get(session).getCommandSubscriptions();
        for (UUID subId : commandSubscriptions) {
            for (CommandSubscription subscription : subscriptionManager.getCommandSubscriptionStorage().get(subId)) {
                if (subscription.getDeviceGuid() != Constants.NULL_SUBSTITUTE) {
                    deviceActivityService.update(subscription.getDeviceGuid());
                }
            }
        }
    }

    @Scheduled(cron = "0/30 * * * * *")
    public synchronized void ping() {
        for (WebSocketSession session : sessionMap.values()) {
            if (session.isOpen()) {
                logger.debug("Pinging session " + session.getId());
                Lock lock = HiveWebsocketSessionState.get(session).getQueueLock();
                lock.lock();
                try {
                    session.sendMessage(new PingMessage(Constants.PING));
                } catch (IOException ex) {
                    logger.error("Error sending ping", ex);
                    closePing(session);
                } finally {
                    lock.unlock();
                }
            } else {
                logger.debug("Session " + session.getId() + " is closed.");
                sessionMap.remove(session.getId());
            }
        }
    }

    public void closePing(WebSocketSession session) {
        try {
            session.close(CloseStatus.NO_CLOSE_FRAME);
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

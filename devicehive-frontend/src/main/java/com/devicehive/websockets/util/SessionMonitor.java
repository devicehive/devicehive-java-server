package com.devicehive.websockets.util;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.vo.DeviceVO;
import com.devicehive.websockets.HiveWebSocketSessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SessionMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SessionMonitor.class);

    @Autowired
    private SubscriptionManager subscriptionManager;
    @Autowired
    private AsyncMessageSupplier asyncMessageSupplier;

    private ConcurrentMap<String, WebSocketSession> sessionMap;

    public void registerSession(final WebSocketSession session) {
        sessionMap.put(session.getId(), session);
    }

    public WebSocketSession getSession(String sessionId) {
        WebSocketSession session = sessionMap.get(sessionId);
        return session != null && session.isOpen() ? session : null;
    }

    public void updateDeviceSession(WebSocketSession session) {
        HivePrincipal hivePrincipal = HiveWebSocketSessionState.get(session).getHivePrincipal();
        DeviceVO authorizedDevice = hivePrincipal != null ? hivePrincipal.getDevice() : null;
        if (authorizedDevice != null) {
            String deviceGuid = authorizedDevice.getGuid();
            //TODO: Replace with RPC call
//            deviceActivityService.update(deviceGuid);
        }
        Set<String> commandSubscriptions = HiveWebSocketSessionState.get(session).getCommandSubscriptions();
        for (String subId : commandSubscriptions) {
            for (CommandSubscription subscription : subscriptionManager.getCommandSubscriptionStorage().get(subId)) {
                if (!Constants.NULL_SUBSTITUTE.equals(subscription.getDeviceGuid())) {
                    //TODO: Replace with RPC call
                    //deviceActivityService.update(subscription.getDeviceGuid());
                }
            }
        }
    }

    @Scheduled(cron = "0/30 * * * * *")
    public synchronized void ping() {
        for (WebSocketSession session : sessionMap.values()) {
            if (session.isOpen()) {
                logger.debug("Pinging session {}", session.getId());
                HiveWebSocketSessionState.get(session).getQueue().offer(AsyncMessageSupplier.PING_JSON_MSG);
                asyncMessageSupplier.deliverMessages(session);
            } else {
                logger.debug("Session {} is closed.", session.getId());
                sessionMap.remove(session.getId());
            }
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

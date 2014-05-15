package com.devicehive.websockets.util;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.Device;
import com.devicehive.service.DeviceActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@Singleton
@ConcurrencyManagement(BEAN)
@EJB(name = "SessionMonitor", beanInterface = SessionMonitor.class)
public class SessionMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SessionMonitor.class);
    private static final String PING = "ping";
    private ConcurrentMap<String, Session> sessionMap;
    @EJB
    private ConfigurationService configurationService;
    @EJB
    private DeviceActivityService deviceActivityService;
    @EJB
    private SubscriptionManager subscriptionManager;

    public void registerSession(final Session session) {
        session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
            @Override
            public void onMessage(PongMessage message) {
                logger.debug("Pong received for session " + session.getId());
                AtomicLong atomicLong = (AtomicLong) session.getUserProperties().get(PING);
                atomicLong.set(System.currentTimeMillis());
                updateDeviceSession(session);
            }
        });
        session.getUserProperties().put(PING, new AtomicLong(System.currentTimeMillis()));
        sessionMap.put(session.getId(), session);
    }

    public Session getSession(String sessionId) {
        Session session = sessionMap.get(sessionId);
        return session != null && session.isOpen() ? session : null;
    }

    private void updateDeviceSession(Session session) {
        HivePrincipal hivePrincipal = WebsocketSession.getPrincipal(session);
        Device authorizedDevice = hivePrincipal != null ? hivePrincipal.getDevice() : null;
        if (authorizedDevice != null) {
            deviceActivityService.update(authorizedDevice.getId());
        }
        Set<UUID> commandSubscriptions = WebsocketSession.getCommandSubscriptions(session);
        for (UUID subId : commandSubscriptions) {
            for (CommandSubscription subscription : subscriptionManager.getCommandSubscriptionStorage().get(subId)) {
                if (subscription.getDeviceId() != Constants.NULL_ID_SUBSTITUTE) {
                    deviceActivityService.update(subscription.getDeviceId());
                }
            }
        }
    }

    @Schedule(hour = "*", minute = "*", second = "*/30", persistent = false)
    public synchronized void ping() {
        for (Session session : sessionMap.values()) {
            if (session.isOpen()) {
                logger.debug("Pinging session " + session.getId());
                try {
                    session.getAsyncRemote().sendPing(Constants.PING);
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

    @Schedule(hour = "*", minute = "*", second = "*/30", persistent = false)
    public synchronized void monitor() {
        Long timeout = configurationService
                .getLong(Constants.WEBSOCKET_SESSION_PING_TIMEOUT, Constants.WEBSOCKET_SESSION_PING_TIMEOUT_DEFAULT);
        for (Session session : sessionMap.values()) {
            logger.debug("Checking session " + session.getId());
            if (session.isOpen()) {
                AtomicLong atomicLong = (AtomicLong) session.getUserProperties().get(PING);
                if (System.currentTimeMillis() - atomicLong.get() > timeout) {
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
            session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, Messages.NO_PONGS_FOR_A_LONG_TIME));
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
                session.close(new CloseReason(CloseReason.CloseCodes.SERVICE_RESTART, Messages.SHUTDOWN));
            } catch (IOException ex) {
                logger.error("Error closing session", ex);
            }
        }
    }
}

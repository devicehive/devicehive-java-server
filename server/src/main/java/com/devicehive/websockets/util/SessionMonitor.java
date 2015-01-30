package com.devicehive.websockets.util;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.Device;
import com.devicehive.service.DeviceActivityService;
import com.devicehive.websockets.HiveWebsocketSessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@Singleton
@ConcurrencyManagement(BEAN)
@EJB(name = "SessionMonitor", beanInterface = SessionMonitor.class)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SessionMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SessionMonitor.class);
    private ConcurrentMap<String, Session> sessionMap;
    @EJB
    private ConfigurationService configurationService;
    @EJB
    private DeviceActivityService deviceActivityService;
    @EJB
    private SubscriptionManager subscriptionManager;

    @EJB
    private SessionMonitor self;

    public void registerSession(final Session session) {
        session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
            @Override
            public void onMessage(PongMessage message) {
                logger.debug("Pong received for session " + session.getId());
                updateDeviceSession(session);
            }
        });
        sessionMap.put(session.getId(), session);
        session.setMaxIdleTimeout(configurationService
                                      .getLong(Constants.WEBSOCKET_SESSION_PING_TIMEOUT,
                                               Constants.WEBSOCKET_SESSION_PING_TIMEOUT_DEFAULT));
        session.setMaxBinaryMessageBufferSize(Constants.WEBSOCKET_MAX_BUFFER_SIZE);
        session.setMaxTextMessageBufferSize(Constants.WEBSOCKET_MAX_BUFFER_SIZE);
    }

    public Session getSession(String sessionId) {
        Session session = sessionMap.get(sessionId);
        return session != null && session.isOpen() ? session : null;
    }

    private void updateDeviceSession(Session session) {
        HivePrincipal hivePrincipal = HiveWebsocketSessionState.get(session).getHivePrincipal();
        Device authorizedDevice = hivePrincipal != null ? hivePrincipal.getDevice() : null;
        if (authorizedDevice != null) {
            //deviceActivityService.update(authorizedDevice.getId());
        }
        Set<UUID> commandSubscriptions = HiveWebsocketSessionState.get(session).getCommandSubscriptions();
        for (UUID subId : commandSubscriptions) {
            for (CommandSubscription subscription : subscriptionManager.getCommandSubscriptionStorage().get(subId)) {
                if (subscription.getDeviceGuid() != Constants.NULL_SUBSTITUTE) {
                    //deviceActivityService.update(subscription.getDeviceId());
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
                    logger.error("Error sending ping", ex);
                    self.closePing(session);
                }
            } else {
                logger.debug("Session " + session.getId() + " is closed.");
                sessionMap.remove(session.getId());
            }
        }
    }


    @Asynchronous
    public void closePing(Session session) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, Messages.PING_ERROR));
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
        sessionMap.clear();
    }
}

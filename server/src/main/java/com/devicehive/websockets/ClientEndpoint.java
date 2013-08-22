package com.devicehive.websockets;

import com.devicehive.json.GsonFactory;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.websockets.handlers.ClientMessageHandlers;
import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebsocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.lang.reflect.InvocationTargetException;

@ServerEndpoint(value = "/websocket/client")
@LogExecutionTime
public class ClientEndpoint extends Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(ClientEndpoint.class);

    @Inject
    private ClientMessageHandlers clientMessageHandlers;
    @EJB
    private SessionMonitor sessionMonitor;

    @EJB
    private SubscriptionManager subscriptionManager;

    @OnOpen
    public void onOpen(Session session) {
        logger.debug("[onOpen] session id {} ", session.getId());
        WebsocketSession.createCommandUpdatesSubscriptionsLock(session);
        WebsocketSession.createNotificationSubscriptionsLock(session);
        WebsocketSession.createQueueLock(session);
        sessionMonitor.registerSession(session);
    }

    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public String onMessage(String rawMessage, Session session) throws InvocationTargetException, IllegalAccessException {
        logger.debug("[onMessage] session id {} ", session.getId());
        return GsonFactory.createGson().toJson(processMessage(clientMessageHandlers, rawMessage, session));
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.debug("[onClose] session id {}, close reason is {} ", session.getId(), closeReason);
        subscriptionManager.getCommandUpdateSubscriptionStorage().removeBySession(session.getId());
        subscriptionManager.getNotificationSubscriptionStorage().removeBySession(session.getId());
    }

    @OnError
    public void onError(Throwable exception, Session session) {
        logger.debug("[onError] ", exception);
    }

}

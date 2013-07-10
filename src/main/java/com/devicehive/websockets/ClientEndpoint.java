package com.devicehive.websockets;


import com.devicehive.websockets.handlers.ClientMessageHandlers;
import com.devicehive.websockets.messagebus.local.subscriptions.dao.CommandSubscriptionDAO;
import com.devicehive.websockets.messagebus.local.subscriptions.dao.CommandUpdatesSubscriptionDAO;
import com.devicehive.websockets.messagebus.local.subscriptions.dao.NotificationSubscriptionDAO;
import com.devicehive.websockets.util.SingletonSessionMap;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.lang.reflect.InvocationTargetException;

@ServerEndpoint(value = "/client")
public class ClientEndpoint extends Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(ClientEndpoint.class);

    @Inject
    private ClientMessageHandlers clientMessageHandlers;

    @Inject
    private SingletonSessionMap sessionMap;

    @Inject
    private CommandSubscriptionDAO commandSubscriptionDAO;

    @Inject
    private CommandUpdatesSubscriptionDAO commandUpdatesSubscriptionDAO;

    @Inject
    private NotificationSubscriptionDAO notificationSubscriptionDAO;

    @OnOpen
    public void onOpen(Session session) {
        logger.debug("[onOpen] session id " + session.getId());
        WebsocketSession.createCommandUpdatesSubscriptionsLock(session);
        WebsocketSession.createNotificationSubscriptionsLock(session);
        WebsocketSession.createQueueLock(session);
        sessionMap.addSession(session);
    }


    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public String onMessage(String rawMessage, Session session) throws InvocationTargetException, IllegalAccessException {
        logger.debug("[onMessage] session id " + session.getId());
        return new GsonBuilder().setPrettyPrinting().create().toJson(processMessage(clientMessageHandlers, rawMessage, session));
    }


    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.debug("[onClose] session id " + session.getId() + ", close reason is " + closeReason);
        sessionMap.deleteSession(session.getId());
        deleteFromSubscriptions(session.getId());
    }

    @OnError
    public void onError(Throwable exception, Session session) {
        logger.debug("[onError] session id " + session.getId(), exception);
    }

    private void deleteFromSubscriptions(String sessionId){
        commandSubscriptionDAO.deleteBySession(sessionId);
        commandUpdatesSubscriptionDAO.deleteBySession(sessionId);
        notificationSubscriptionDAO.deleteBySession(sessionId);
    }


}

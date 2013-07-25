package com.devicehive.websockets;

import java.lang.reflect.InvocationTargetException;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.messages.MessageDetails;
import com.devicehive.messages.MessageType;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.websockets.handlers.ClientMessageHandlers;
import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.GsonBuilder;

@ServerEndpoint(value = "/client")
public class ClientEndpoint extends Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(ClientEndpoint.class);

    @Inject
    private ClientMessageHandlers clientMessageHandlers;
    @EJB
    private SessionMonitor sessionMonitor;
    @Inject
    private MessageBus messageBus;

    @OnOpen
    public void onOpen(Session session) {
        logger.debug("[onOpen] session id " + session.getId());
        WebsocketSession.createCommandUpdatesSubscriptionsLock(session);
        WebsocketSession.createNotificationSubscriptionsLock(session);
        WebsocketSession.createQueueLock(session);
        sessionMonitor.registerSession(session);
    }

    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public String onMessage(String rawMessage, Session session) throws InvocationTargetException, IllegalAccessException {
        logger.debug("[onMessage] session id " + session.getId());
        return new GsonBuilder().setPrettyPrinting().create().toJson(processMessage(clientMessageHandlers, rawMessage, session));
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.debug("[onClose] session id " + session.getId() + ", close reason is " + closeReason);
        messageBus.unsubscribe(MessageType.CLOSED_SESSION_CLIENT, MessageDetails.create().session(session.getId()));
    }

    @OnError
    public void onError(Throwable exception, Session session) {
        logger.debug("[onError] session id " + session.getId(), exception);
    }

}

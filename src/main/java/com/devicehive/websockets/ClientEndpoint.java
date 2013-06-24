package com.devicehive.websockets;


import com.devicehive.websockets.handlers.ClientMessageHandlers;
import com.devicehive.websockets.util.WebsocketSession;
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


    @OnOpen
    public void onOpen(Session session) {
        logger.debug("[onOpen] session id " + session.getId());
        WebsocketSession.createCommandUpdatesSubscriptionsLock(session);
    }


    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public String onMessage(String rawMessage, Session session) throws InvocationTargetException, IllegalAccessException {
        logger.debug("[onMessage] session id " + session.getId());
        return processMessage(clientMessageHandlers, rawMessage, session).toString();
    }


    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.debug("[onClose] session id " + session.getId() + ", close reason is " + closeReason);
    }

    @OnError
    public void onError(Throwable exception, Session session) {
        logger.debug("[onError] session id " + session.getId(), exception);
    }


}

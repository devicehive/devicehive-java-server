package com.devicehive.websockets;


import com.devicehive.websockets.handlers.DeviceMessageHandlers;
import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.lang.reflect.InvocationTargetException;

@ServerEndpoint(value = "/device")
public class DeviceEndpoint extends Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(DeviceEndpoint.class);

    @Inject
    private DeviceMessageHandlers deviceMessageHandlers;


    @EJB
    private SessionMonitor sessionMonitor;


    @OnOpen
    public void onOpen(Session session) {
        logger.debug("[onOpen] session id " + session.getId());
        WebsocketSession.createCommandsSubscriptionsLock(session);
        WebsocketSession.createQueueLock(session);
        sessionMonitor.registerSession(session);
    }

    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public String onMessage(String message, Session session) throws InvocationTargetException, IllegalAccessException {
        logger.debug("[onMessage] session id " + session.getId());
        return new GsonBuilder().setPrettyPrinting().create().toJson(processMessage(deviceMessageHandlers, message, session));
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

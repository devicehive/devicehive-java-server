package com.devicehive.websockets;


import com.devicehive.dao.DeviceDAO;
import com.devicehive.websockets.handlers.DeviceMessageHandlers;
import com.devicehive.websockets.util.SingletonSessionMap;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.lang.reflect.InvocationTargetException;

@ServerEndpoint(value = "/device")
public class DeviceEndpoint extends Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(DeviceEndpoint.class);

    @Inject
    private DeviceMessageHandlers deviceMessageHandlers;

    @Inject
    private DeviceDAO deviceDAO;

    @Inject
    private SingletonSessionMap sessionMap;


    @OnOpen
    public void onOpen(Session session) {
        logger.debug("[onOpen] session id " + session.getId());
        WebsocketSession.createCommandsSubscriptionsLock(session);
        sessionMap.addSession(session);
    }

    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public String onMessage(String message, Session session) throws InvocationTargetException, IllegalAccessException {
        logger.debug("[onMessage] session id " + session.getId());
        return new GsonBuilder().setPrettyPrinting().create().toJson(processMessage(deviceMessageHandlers, message, session));
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.debug("[onClose] session id " + session.getId() + ", close reason is " + closeReason);
        sessionMap.deleteSession(session.getId());
    }

    @OnError
    public void onError(Throwable exception, Session session) {
        logger.debug("[onError] session id " + session.getId(), exception);
    }




}

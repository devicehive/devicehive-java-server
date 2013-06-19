package com.devicehive.websockets;


import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveWebsocketException;
import com.devicehive.model.Device;
import com.devicehive.websockets.handlers.DeviceMessageHandlers;
import com.devicehive.websockets.handlers.HiveMessageHandlers;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import com.devicehive.websockets.json.*;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

@ServerEndpoint(value = "/device")
public class DeviceEndpoint extends Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(DeviceEndpoint.class);

    @Inject
    private DeviceMessageHandlers deviceMessageHandlers;

    @Inject
    private DeviceDAO deviceDAO;


    @OnOpen
    public void onOpen(Session session) {
        logger.debug("[onOpen] session id " + session.getId());
    }

    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public String onMessage(String message, Session session) throws InvocationTargetException, IllegalAccessException {
        logger.debug("[onMessage] session id " + session.getId());
        return processMessage(message, session).toString();
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.debug("[onClose] session id " + session.getId() + ", close reason is " + closeReason);
    }

    @OnError
    public void onError(Throwable exception, Session session) {
        logger.debug("[onError] session id " + session.getId(), exception);
    }

    @Override
    protected HiveMessageHandlers getHiveMessageHandler() {
        return deviceMessageHandlers;
    }


    @PostConstruct
    public void postConstruct() {
        super.postConstruct();
    }


}

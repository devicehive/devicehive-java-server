package com.devicehive.websockets;


import com.devicehive.websockets.handlers.DeviceMessageHandlers;
import com.devicehive.websockets.handlers.HiveMessageHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import com.devicehive.websockets.json.*;

import java.lang.reflect.InvocationTargetException;


@ServerEndpoint(value = "/device", encoders = {JsonWebsocketEncoder.class}, decoders = {JsonWebsocketDecoder.class})
public class DeviceEndpoint extends Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(DeviceEndpoint.class);


    @OnOpen
    public void onOpen(Session session) {
        logger.debug("[onOpen] session id " + session.getId());
    }

    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public String onMessage(JsonObject message, Session session) throws InvocationTargetException, IllegalAccessException {
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
        return new DeviceMessageHandlers();
    }
}

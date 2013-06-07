package com.devicehive.websockets;


import com.devicehive.websockets.handlers.ClientMessageHandlers;
import com.devicehive.websockets.handlers.HiveMessageHandlers;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import com.devicehive.websockets.json.*;

import java.lang.reflect.InvocationTargetException;


@ServerEndpoint(value = "/client", encoders = {JsonWebsocketEncoder.class}, decoders = {JsonWebsocketDecoder.class})
public class ClientEndpoint extends Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(ClientEndpoint.class);


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
        return new ClientMessageHandlers();
    }
}

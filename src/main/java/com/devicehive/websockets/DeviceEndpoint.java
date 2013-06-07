package com.devicehive.websockets;


import com.devicehive.websockets.handlers.DeviceMessageHandlers;
import com.devicehive.websockets.handlers.HiveMessageHandlers;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import com.devicehive.websockets.json.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;


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

    @Override
    protected boolean checkAuth(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson();
        /*
            TODO check session auth
         */
        UUID deviceId = gson.fromJson(message.get("deviceId"), UUID.class);
        String deviceKey = message.get("deviceKey").getAsString();
        /*
            TODO check explicit credentials
         */
        return false;
    }


}

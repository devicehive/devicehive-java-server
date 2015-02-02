package com.devicehive.websockets;


import com.google.gson.JsonObject;

import com.devicehive.websockets.converters.JsonEncoder;
import com.devicehive.websockets.util.HiveEndpoint;

import java.io.Reader;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/websocket/device", encoders = {JsonEncoder.class}, configurator = HiveConfigurator.class)
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class HiveDeviceEndpoint extends HiveServerEndpoint {

    @Override
    @OnOpen
    public void onOpen(Session session) {
        super.onOpen(session);
        HiveWebsocketSessionState state =
            (HiveWebsocketSessionState) session.getUserProperties().get(HiveWebsocketSessionState.KEY);
        state.setEndpoint(HiveEndpoint.DEVICE);
    }

    @Override
    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public JsonObject onMessage(Reader reader, Session session) {
        return super.onMessage(reader, session);
    }

    @Override
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
    }

    @Override
    @OnError
    public void onError(Throwable exception, Session session) {
        super.onError(exception, session);
    }

}

package com.devicehive.websockets;

import com.devicehive.websockets.converters.JsonEncoder;
import com.devicehive.websockets.util.HiveEndpoint;
import com.google.gson.JsonObject;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.Reader;

@ServerEndpoint(value = "/websocket/client", encoders = {JsonEncoder.class}, configurator = HiveConfigurator.class)
public class HiveClientEndpoint extends HiveServerEndpoint {

    @Override
    @OnOpen
    public void onOpen(Session session) {
        super.onOpen(session);
        HiveWebsocketSessionState state =
            (HiveWebsocketSessionState) session.getUserProperties().get(HiveWebsocketSessionState.KEY);
        state.setEndpoint(HiveEndpoint.CLIENT);
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

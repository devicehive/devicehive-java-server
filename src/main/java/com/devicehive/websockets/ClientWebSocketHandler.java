package com.devicehive.websockets;

import com.devicehive.application.websocket.WebSocketAuthenticationManager;
import com.devicehive.websockets.util.HiveEndpoint;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class ClientWebSocketHandler extends AbstractWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        HiveWebsocketSessionState state =
                (HiveWebsocketSessionState) session.getAttributes().get(HiveWebsocketSessionState.KEY);
        session.getAttributes().put(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION, session.getPrincipal());
        state.setEndpoint(HiveEndpoint.CLIENT);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
    }
}

package com.devicehive.websockets;

import com.devicehive.websockets.util.HiveEndpoint;
import org.springframework.web.socket.WebSocketSession;

public class DeviceWebSocketHandler extends AbstractWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        HiveWebsocketSessionState state =
                (HiveWebsocketSessionState) session.getAttributes().get(HiveWebsocketSessionState.KEY);
        session.getAttributes().put(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION, session.getPrincipal());
        state.setEndpoint(HiveEndpoint.DEVICE);
    }
}

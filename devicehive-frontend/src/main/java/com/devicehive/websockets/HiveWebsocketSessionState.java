package com.devicehive.websockets;


import com.devicehive.auth.HivePrincipal;
import org.springframework.web.socket.WebSocketSession;

public class HiveWebsocketSessionState {

    public static final String KEY = HiveWebsocketSessionState.class.getName();
    private HivePrincipal hivePrincipal;

    public static HiveWebsocketSessionState get(WebSocketSession session) {
        return (HiveWebsocketSessionState) session.getAttributes().get(HiveWebsocketSessionState.KEY);
    }


    public HivePrincipal getHivePrincipal() {
        return hivePrincipal;
    }

    public void setHivePrincipal(HivePrincipal hivePrincipal) {
        this.hivePrincipal = hivePrincipal;
    }
}

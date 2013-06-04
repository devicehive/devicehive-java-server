package com.devicehive.websockets;


import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.logging.Level;

@ServerEndpoint(value = "/client")
public class ClientSocketService {

    @OnOpen
    public void onOpen(Session session) {

    }


    @OnClose
    public void onClose(Session session, CloseReason closeReason) {

    }

    @OnError
    public void onError(Throwable exception, Session session) {

    }
}

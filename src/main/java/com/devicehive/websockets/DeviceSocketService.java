package com.devicehive.websockets;


import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/device")
public class DeviceSocketService {

    @OnOpen
    public void onOpen(Session session) {

    }


    /*
     TODO choose input parameter type: Reader?
    @OnMessage(maxMessageSize = 5000)
    public String onMessage(String str, Session session) {

        return str;
    }
    */

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {

    }

    @OnError
    public void onError(Throwable exception, Session session) {

    }
}

package com.devicehive.base;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

public abstract class AbstractWebSocketTest extends AbstractResourceTest {

    protected static final int WAIT_TIMEOUT = 3;

    protected SynchronousWebSocketClientHandler syncClientHandler(String path) throws InterruptedException {
        StandardWebSocketClient client = new StandardWebSocketClient();
        SynchronousWebSocketClientHandler handler = new SynchronousWebSocketClientHandler();
        WebSocketConnectionManager manager = new WebSocketConnectionManager(client, handler, wsBaseUri() + path);
        manager.start();
        handler.awaitMessage(WAIT_TIMEOUT, new TextMessage("connected"));
        return handler;
    }
}

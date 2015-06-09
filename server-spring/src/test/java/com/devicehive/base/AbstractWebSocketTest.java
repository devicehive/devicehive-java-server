package com.devicehive.base;

import com.devicehive.base.websocket.WebSocketClientHandler;
import com.devicehive.base.websocket.WebSocketSynchronousConnection;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

public abstract class AbstractWebSocketTest extends AbstractResourceTest {

    protected WebSocketSynchronousConnection syncConnection(String path) throws InterruptedException {
        WebSocketSynchronousConnection connection = new WebSocketSynchronousConnection();
        connection.start(wsBaseUri() + path);
        return connection;
    }
}

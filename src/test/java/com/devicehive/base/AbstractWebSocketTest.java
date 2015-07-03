package com.devicehive.base;

import com.devicehive.base.websocket.WebSocketSynchronousConnection;

public abstract class AbstractWebSocketTest extends AbstractResourceTest {

    protected WebSocketSynchronousConnection syncConnection(String path) throws InterruptedException {
        WebSocketSynchronousConnection connection = new WebSocketSynchronousConnection();
        connection.start(wsBaseUri() + path);
        return connection;
    }
}

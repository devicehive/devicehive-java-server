package com.devicehive.base;

import com.devicehive.base.websocket.WebSocketSynchronousConnection;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractWebSocketTest extends AbstractResourceTest {

    private final List<WebSocketSynchronousConnection> connections = Collections.synchronizedList(new LinkedList<>());

    protected WebSocketSynchronousConnection syncConnection(String path) throws InterruptedException {
        WebSocketSynchronousConnection connection = new WebSocketSynchronousConnection();
        connection.start(wsBaseUri() + path);
        connections.add(connection);
        return connection;
    }

    protected void clearWSConnections() {
        connections.forEach(WebSocketSynchronousConnection::stop);
        connections.clear();
    }
}

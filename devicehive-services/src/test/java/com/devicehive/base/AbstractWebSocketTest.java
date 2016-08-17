package com.devicehive.base;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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

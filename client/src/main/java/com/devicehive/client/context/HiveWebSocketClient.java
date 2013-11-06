package com.devicehive.client.context;


import com.devicehive.client.websocket.HiveClientEndpoint;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

public class HiveWebSocketClient implements Closeable{

    private HiveClientEndpoint endpoint;
    private final URI socket;
    private final HiveContext hiveContext;

    public HiveWebSocketClient(URI socket, HiveContext hiveContext) {
        this.socket = socket;
        this.hiveContext = hiveContext;
        endpoint = new HiveClientEndpoint(socket);
    }

    public void sendMessage(){

    }

    public void receiveMessage(){

    }

    @Override
    public void close() throws IOException {

    }
}

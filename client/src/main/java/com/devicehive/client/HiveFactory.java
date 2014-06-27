package com.devicehive.client;


import com.devicehive.client.impl.HiveClientRestImpl;
import com.devicehive.client.impl.HiveClientWebsocketImpl;
import com.devicehive.client.impl.HiveDeviceRestImpl;
import com.devicehive.client.impl.HiveDeviceWebsocketImpl;
import com.devicehive.client.impl.context.RestAgent;
import com.devicehive.client.impl.context.WebsocketAgent;
import com.devicehive.client.impl.context.connection.HiveConnectionEventHandler;
import com.devicehive.client.model.exceptions.HiveException;

import java.net.URI;

public class HiveFactory {

    private HiveFactory() {
    }

    public static HiveClient createClient(URI restUri,
                                          boolean preferWebsockets) throws HiveException {
        return createClient(restUri, preferWebsockets, null, null);
    }

    public static HiveClient createClient(URI restUri,
                                          boolean preferWebsockets,
                                          ConnectionEstablishedNotifier connectionEstablishedNotifier,
                                          ConnectionLostNotifier connectionLostNotifier) throws HiveException {
        HiveConnectionEventHandler connectionEventHandler = new HiveConnectionEventHandler(connectionLostNotifier, connectionEstablishedNotifier);
        if (preferWebsockets) {
            return new HiveClientWebsocketImpl(createWebsocketCleintAgent(restUri, connectionEventHandler));
        } else {
            return new HiveClientRestImpl(createRestAgent(restUri, connectionEventHandler));
        }
    }

    public static HiveDevice createDevice(URI restUri,
                                          boolean preferWebsockets) throws HiveException {
        return createDevice(restUri, preferWebsockets, null, null);
    }

    public static HiveDevice createDevice(URI restUri,
                                          boolean preferWebsockets,
                                          ConnectionEstablishedNotifier connectionEstablishedNotifier,
                                          ConnectionLostNotifier connectionLostNotifier) throws HiveException {
        HiveConnectionEventHandler connectionEventHandler = new HiveConnectionEventHandler(connectionLostNotifier, connectionEstablishedNotifier);
        if (preferWebsockets) {
            return new HiveDeviceWebsocketImpl(createWebsocketDeviceAgent(restUri, connectionEventHandler));
        } else {
            return new HiveDeviceRestImpl(createRestAgent(restUri, connectionEventHandler));
        }
    }


    private static RestAgent createRestAgent(URI restUri, HiveConnectionEventHandler connectionEventHandler) throws HiveException {
        RestAgent agent  = new RestAgent(restUri, connectionEventHandler);
        agent.connect();
        return agent;
    }

    private static WebsocketAgent createWebsocketCleintAgent(URI restUri, HiveConnectionEventHandler connectionEventHandler) throws HiveException {
        WebsocketAgent agent = new WebsocketAgent(restUri, "client", connectionEventHandler);
        agent.connect();
        return agent;
    }

    private static WebsocketAgent createWebsocketDeviceAgent(URI restUri, HiveConnectionEventHandler connectionEventHandler) throws HiveException {
        WebsocketAgent agent = new WebsocketAgent(restUri, "device", connectionEventHandler);
        agent.connect();
        return agent;
    }
}

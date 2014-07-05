package com.devicehive.client;


import com.devicehive.client.impl.HiveClientRestImpl;
import com.devicehive.client.impl.HiveClientWebsocketImpl;
import com.devicehive.client.impl.HiveDeviceRestImpl;
import com.devicehive.client.impl.HiveDeviceWebsocketImpl;
import com.devicehive.client.impl.context.RestAgent;
import com.devicehive.client.impl.context.WebsocketAgent;
import com.devicehive.client.model.exceptions.HiveException;

import java.net.URI;

public class HiveFactory {

    private HiveFactory() {
    }


    public static HiveClient createClient(URI restUri,
                                          boolean preferWebsockets) throws HiveException {
        if (preferWebsockets) {
            return new HiveClientWebsocketImpl(createWebsocketClientAgent(restUri));
        } else {
            return new HiveClientRestImpl(createRestAgent(restUri));
        }
    }


    public static HiveDevice createDevice(URI restUri,
                                          boolean preferWebsockets) throws HiveException {
        if (preferWebsockets) {
            return new HiveDeviceWebsocketImpl(createWebsocketDeviceAgent(restUri));
        } else {
            return new HiveDeviceRestImpl(createRestAgent(restUri));
        }
    }

    private static RestAgent createRestAgent(URI restUri)
            throws HiveException {
        RestAgent agent = new RestAgent(restUri);
        agent.connect();
        return agent;
    }

    private static WebsocketAgent createWebsocketClientAgent(URI restUri)
            throws HiveException {
        WebsocketAgent agent = new WebsocketAgent(restUri, "client");
        agent.connect();
        return agent;
    }

    private static WebsocketAgent createWebsocketDeviceAgent(URI restUri)
            throws HiveException {
        WebsocketAgent agent = new WebsocketAgent(restUri, "device");
        agent.connect();
        return agent;
    }
}

package com.devicehive.client;


import com.devicehive.client.impl.HiveClientRestImpl;
import com.devicehive.client.impl.HiveClientWebsocketImpl;
import com.devicehive.client.impl.HiveDeviceRestImpl;
import com.devicehive.client.impl.HiveDeviceWebsocketImpl;
import com.devicehive.client.impl.context.HiveRestContext;
import com.devicehive.client.impl.context.HiveWebsocketContext;
import com.devicehive.client.impl.context.connection.HiveConnectionEventHandler;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.exceptions.HiveException;

import java.net.URI;

public class HiveFactory {

    private HiveFactory() {
    }

    public static HiveClient createClient(URI restUri,
                                          boolean preferWebsockets,
                                          HiveMessageHandler<DeviceCommand> commandUpdatesHandler) throws HiveException {
        if (preferWebsockets) {
            HiveWebsocketContext context = new HiveWebsocketContext(restUri, commandUpdatesHandler, null);
            return new HiveClientWebsocketImpl(context);
        } else {
            HiveRestContext hiveContext = new HiveRestContext(restUri, commandUpdatesHandler, null);
            return new HiveClientRestImpl(hiveContext);
        }
    }

    public static HiveClient createClient(URI restUri,
                                          boolean preferWebsockets,
                                          HiveMessageHandler<DeviceCommand> commandUpdatesHandler,
                                          HiveConnectionEventHandler connectionEventHandler) throws HiveException {
        if (preferWebsockets) {
            HiveWebsocketContext context =
                    new HiveWebsocketContext(restUri, commandUpdatesHandler, connectionEventHandler);
            return new HiveClientWebsocketImpl(context);
        } else {
            HiveRestContext hiveContext = new HiveRestContext(restUri, commandUpdatesHandler, connectionEventHandler);
            return new HiveClientRestImpl(hiveContext);
        }
    }

    public static HiveDevice createDevice(URI restUri,
                                          boolean preferWebsockets,
                                          HiveMessageHandler<DeviceCommand> commandUpdatesHandler) throws HiveException {
        if (preferWebsockets) {
            HiveWebsocketContext context =
                    new HiveWebsocketContext(restUri, commandUpdatesHandler, null);
            return new HiveDeviceWebsocketImpl(context);
        } else {
            HiveRestContext hiveContext = new HiveRestContext(restUri, commandUpdatesHandler, null);
            return new HiveDeviceRestImpl(hiveContext);
        }
    }

    public static HiveDevice createDevice(URI restUri,
                                          boolean preferWebsockets,
                                          HiveMessageHandler<DeviceCommand> commandUpdatesHandler,
                                          HiveConnectionEventHandler connectionEventHandler) throws HiveException {
        if (preferWebsockets) {
            HiveWebsocketContext context =
                    new HiveWebsocketContext(restUri, commandUpdatesHandler, connectionEventHandler);
            return new HiveDeviceWebsocketImpl(context);
        } else {
            HiveRestContext hiveContext = new HiveRestContext(restUri, commandUpdatesHandler, connectionEventHandler);
            return new HiveDeviceRestImpl(hiveContext);
        }
    }
}

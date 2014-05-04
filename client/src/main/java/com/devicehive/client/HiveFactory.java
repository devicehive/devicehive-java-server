package com.devicehive.client;


import com.devicehive.client.impl.HiveClientRestImpl;
import com.devicehive.client.impl.HiveClientWebsocketImpl;
import com.devicehive.client.impl.HiveDeviceRestImpl;
import com.devicehive.client.impl.HiveDeviceWebsocketImpl;
import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.model.Role;
import com.devicehive.client.model.exceptions.HiveException;

import java.net.URI;

public class HiveFactory {

    private HiveFactory() {
    }

    public static HiveClient createClient(URI restUri,
                                          boolean useWebsockets,
                                          CommandsHandler commandsHandler,
                                          NotificationsHandler notificationsHandler) throws HiveException {
        HiveContext context =
                new HiveContext(useWebsockets, restUri, Role.USER, null, null, commandsHandler, notificationsHandler);
        return useWebsockets
                ? new HiveClientWebsocketImpl(context)
                : new HiveClientRestImpl(context);
    }

    public static HiveClient createClient(URI restUri,
                                          CommandsHandler commandsHandler,
                                          NotificationsHandler notificationsHandler) throws HiveException {
        return createClient(restUri, true, commandsHandler, notificationsHandler);
    }

    public static HiveClient createRESTOnlyClient(URI restUri,
                                                  CommandsHandler commandsHandler,
                                                  NotificationsHandler notificationsHandler) throws HiveException {
        return createClient(restUri, false, commandsHandler, notificationsHandler);
    }

    public static HiveDevice createDevice(URI restUri,
                                          boolean useWebsockets,
                                          CommandsHandler commandsHandler,
                                          NotificationsHandler notificationsHandler) throws HiveException {
        HiveContext context =
                new HiveContext(useWebsockets, restUri, Role.DEVICE, null, null, commandsHandler, notificationsHandler);
        return useWebsockets
                ? new HiveDeviceWebsocketImpl(context)
                : new HiveDeviceRestImpl(context);
    }

    public static HiveDevice createDevice(URI restUri,
                                          CommandsHandler commandsHandler,
                                          NotificationsHandler notificationsHandler) throws HiveException {
        return createDevice(restUri, true, commandsHandler, notificationsHandler);
    }

    public static HiveDevice createRESTOnlyDevice(URI restUri,
                                                  CommandsHandler commandsHandler,
                                                  NotificationsHandler notificationsHandler) throws HiveException {
        return createDevice(restUri, false, commandsHandler, notificationsHandler);
    }
}

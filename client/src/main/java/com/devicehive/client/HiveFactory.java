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

    public HiveClient createClient(URI restUri, boolean useWebsockets) throws HiveException {
        HiveContext context = new HiveContext(useWebsockets, restUri, Role.USER, null, null);
        return useWebsockets
                ? new HiveClientWebsocketImpl(context)
                : new HiveClientRestImpl(context);
    }

    public HiveClient createClient(URI restUri) throws HiveException {
        return createClient(restUri, true);
    }

    public HiveClient createRESTOnlyClient(URI restUri) throws HiveException {
        return createClient(restUri, false);
    }

    public HiveDevice createDevice(URI restUri, boolean useWebsockets) throws HiveException {
        HiveContext context = new HiveContext(useWebsockets, restUri, Role.DEVICE, null, null);
        return useWebsockets
                ? new HiveDeviceRestImpl(context)
                : new HiveDeviceWebsocketImpl(context);
    }

    public HiveDevice createDevice(URI restUri) throws HiveException {
        return createDevice(restUri, true);
    }

    public HiveDevice createRESTOnlyDevice(URI restUri) throws HiveException {
        return createDevice(restUri, false);
    }
}

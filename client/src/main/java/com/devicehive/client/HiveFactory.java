package com.devicehive.client;


import com.devicehive.client.impl.HiveClientRestImpl;
import com.devicehive.client.impl.HiveClientWebsocketImpl;
import com.devicehive.client.impl.context.RestHiveContext;
import com.devicehive.client.impl.context.WebsocketHiveContext;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.exceptions.HiveException;

import java.net.URI;

public class HiveFactory {

    private HiveFactory() {
    }

    public static HiveClient createClient(URI restUri,
                                          boolean preferWebsockets,
                                          MessageHandler<DeviceCommand> commandUpdatesHandler) throws HiveException {
         if (preferWebsockets){
             WebsocketHiveContext context = new WebsocketHiveContext(restUri, commandUpdatesHandler);
             return new HiveClientWebsocketImpl(context);
         } else {
             RestHiveContext hiveContext = new RestHiveContext(restUri, commandUpdatesHandler);
             return new HiveClientRestImpl(hiveContext);
         }
    }

    public static HiveDevice createDevice(URI restUri,
                                          boolean useWebsockets,
                                          MessageHandler<DeviceCommand> commandUpdatesHandler) throws HiveException {
        //TODO
        return null;
    }
}

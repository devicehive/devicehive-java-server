package com.devicehive.client;


import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.model.Role;

import java.net.URI;

public class HiveFactory {

    public HiveClient createClient(URI restUri, boolean useWebsockets) {
        HiveContext context = new HiveContext(useWebsockets, restUri, Role.USER, null, null);

    }
}

package com.devicehive.client.rest;


import com.devicehive.client.rest.providers.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class HiveClientFactory {

    public static Client getClient() {
        Client client = ClientBuilder.newClient();
        client.register(JsonRawProvider.class);
        client.register(HiveEntityProvider.class);
        client.register(TimestampConverterProvider.class);
        client.register(CollectionProvider.class);
        return client;
    }
}

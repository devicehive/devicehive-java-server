package com.devicehive.client.rest;


import com.devicehive.client.rest.providers.CollectionProvider;
import com.devicehive.client.rest.providers.HiveEntityProvider;
import com.devicehive.client.rest.providers.JsonRawProvider;
import com.devicehive.client.rest.providers.TimestampConverterProvider;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * RESTful client factory
 */
public class HiveClientFactory {

    /**
     * @return Jersey client with entity body parse providers, parameters converters.
     */
    public static Client getClient() {
        Client client = ClientBuilder.newClient();
        client.register(JsonRawProvider.class);
        client.register(HiveEntityProvider.class);
        client.register(TimestampConverterProvider.class);
        client.register(CollectionProvider.class);
        return client;
    }
}

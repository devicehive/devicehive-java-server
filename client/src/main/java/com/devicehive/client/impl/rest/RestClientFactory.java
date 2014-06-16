package com.devicehive.client.impl.rest;


import com.devicehive.client.impl.rest.providers.CollectionProvider;
import com.devicehive.client.impl.rest.providers.HiveEntityProvider;
import com.devicehive.client.impl.rest.providers.JsonRawProvider;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.ws.rs.client.Client;

/**
 * RESTful client factory
 */
public class RestClientFactory {

    /**
     * @return Jersey client with entity body parse providers, parameters converters.
     */
    public static Client getClient() {
        JerseyClient client = JerseyClientBuilder.createClient();
        return client.register(JsonRawProvider.class)
                .register(HiveEntityProvider.class)
                .register(CollectionProvider.class);

    }
}

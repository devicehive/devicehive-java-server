package com.devicehive.client.impl.context;


import com.devicehive.client.model.Role;

import java.net.URI;

public class ConnectionDescriptor {

    private final URI restURI;
    private final URI websocketURI;


    public ConnectionDescriptor(URI restURI, URI websocketURI) {
        this.restURI = restURI;
        this.websocketURI = websocketURI;
    }

    public URI getRestURI() {
        return restURI;
    }

    public URI getWebsocketURI() {
        return websocketURI;
    }
}

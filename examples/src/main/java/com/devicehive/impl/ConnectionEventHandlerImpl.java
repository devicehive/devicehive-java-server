package com.devicehive.impl;


import com.devicehive.client.ConnectionEstablishedNotifier;
import com.devicehive.client.ConnectionLostNotifier;

import java.net.URI;
import java.sql.Timestamp;

public class ConnectionEventHandlerImpl implements ConnectionEstablishedNotifier, ConnectionLostNotifier{

    @Override
    public void notify(Timestamp timestamp, String s, URI uri) {
        String message = String.format("Connection event happend: %s, id: %s, uri: %s", timestamp.toString(), s, uri);
        System.out.println(message);
    }
}

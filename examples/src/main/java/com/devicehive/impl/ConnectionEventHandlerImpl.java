package com.devicehive.impl;


import com.devicehive.client.impl.context.connection.ConnectionEstablishedNotifier;
import com.devicehive.client.impl.context.connection.ConnectionLostNotifier;

import java.net.URI;
import java.sql.Timestamp;

public class ConnectionEventHandlerImpl implements ConnectionEstablishedNotifier, ConnectionLostNotifier{

    @Override
    public void notify(Timestamp timestamp, String s, URI uri) {
        String message = String.format("Lost at: %s, id: %s, uri: %s", timestamp.toString(), s, uri);
        System.out.print(message);
    }
}

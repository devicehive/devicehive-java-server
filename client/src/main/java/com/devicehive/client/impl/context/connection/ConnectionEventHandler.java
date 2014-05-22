package com.devicehive.client.impl.context.connection;


import com.devicehive.client.model.exceptions.InternalHiveClientException;

public interface ConnectionEventHandler {
    void handle(ConnectionEvent event) throws InternalHiveClientException;
}

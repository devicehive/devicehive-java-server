package com.devicehive.client;

import java.net.URI;
import java.sql.Timestamp;

/**
 * Optional. Notifies that device or client established the connection after its loosing
 */
public interface ConnectionEstablishedNotifier {

    /**
     * Notifies that reconnection for device or key proceed successfully
     *
     * @param timestamp timestamp of connection lost event
     * @param id        device or key identifier
     */
    void notify(Timestamp timestamp, String id, URI serviceURI);

}

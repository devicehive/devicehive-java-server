package com.devicehive.client.impl.util.connection;

import java.net.URI;
import java.sql.Timestamp;

/**
 * Optional. Notifies that device or client has lost the connection to the server
 */
public interface ConnectionLostNotifier {

    /**
     * Notifies that device or key lost connection
     * @param timestamp timestamp of connection lost event
     * @param id device or key identifier
     */
    void notify(Timestamp timestamp, String id, URI serviceURI);

}

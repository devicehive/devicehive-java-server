package com.devicehive.client.util.connection;

import com.devicehive.client.model.exceptions.InternalHiveClientException;
import org.apache.commons.lang3.ObjectUtils;

import java.net.URI;
import java.sql.Timestamp;

/**
 * TODO
 */
public class ConnectionEvent {
    private boolean isLost;
    private Timestamp timestamp;
    private String id;
    private URI serviceUri;

    public ConnectionEvent(URI serviceUri, Timestamp timestamp, String id) {
        this.serviceUri = serviceUri;
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
        this.id = id;
    }

    public URI getServiceUri() {
        return serviceUri;
    }

    public boolean isLost() {
        return isLost;
    }

    public void setLost(boolean lost) {
        isLost = lost;
    }

    public Timestamp getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (this.id == null) {
            this.id = id;
        } else {
            throw new InternalHiveClientException("Id is already set!");
        }
    }

}

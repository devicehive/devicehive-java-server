package com.devicehive.client.impl.context.connection;

import com.devicehive.client.impl.context.HivePrincipal;
import com.devicehive.client.model.exceptions.InternalHiveClientException;
import org.apache.commons.lang3.ObjectUtils;

import java.net.URI;
import java.sql.Timestamp;

/**
 * Connection event that raises up on connection lost or connection established events. Contains info about
 * connection lost/established time, client id (for user it is access key or login,
 * for device it id device identifier), service URI (REST and websocket URI are differ from each other)
 */
public class ConnectionEvent {
    private boolean isLost;
    private Timestamp timestamp;
    private String id;
    private URI serviceUri;

    public ConnectionEvent(URI serviceUri, Timestamp timestamp, HivePrincipal principal) {
        this.serviceUri = serviceUri;
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
        if (principal != null) {
            if (principal.getDevice() != null) {
                id = principal.getDevice().getLeft();
            } else if (principal.getAccessKey() != null) {
                id = principal.getAccessKey();
            } else if (principal.getUser() != null) {
                id = principal.getUser().getLeft();
            }
        }
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

    public void setId(String id) throws InternalHiveClientException {
        if (this.id == null) {
            this.id = id;
        } else {
            throw new InternalHiveClientException("Id is already set!");
        }
    }

}

package com.devicehive.client.model;


import com.devicehive.client.model.exceptions.HiveClientException;

import javax.ws.rs.core.Response;

public enum AccessType {
    ONLINE("Online"),
    OFFLINE("Offline");
    private final String value;

    AccessType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.name();
    }

    public static AccessType forName(String value) {
        for (AccessType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new HiveClientException("Illegal argument: " + value, Response.Status.BAD_REQUEST.getStatusCode());

    }
}

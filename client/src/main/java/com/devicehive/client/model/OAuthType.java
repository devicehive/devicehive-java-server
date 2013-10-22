package com.devicehive.client.model;


import com.devicehive.client.model.exceptions.HiveClientException;

import javax.ws.rs.core.Response;

public enum OAuthType {
    CODE("Code"),
    TOKEN("Token"),
    PASSWORD("Password");
    private final String value;

    OAuthType(String value) {
        this.value = value;
    }

    public static OAuthType forName(String value) {
        for (OAuthType OAuthType : values()) {
            if (OAuthType.value.equals(value)) {
                return OAuthType;
            }
        }
        throw new HiveClientException("Illegal argument: " + value, Response.Status.BAD_REQUEST.getStatusCode());

    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}

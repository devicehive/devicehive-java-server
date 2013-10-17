package com.devicehive.model;

import com.devicehive.exceptions.HiveException;

import javax.servlet.http.HttpServletResponse;

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
        throw new HiveException("Illegal argument: " + value, HttpServletResponse.SC_BAD_REQUEST);

    }
}

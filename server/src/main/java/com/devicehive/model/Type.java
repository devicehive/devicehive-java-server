package com.devicehive.model;


import com.devicehive.exceptions.HiveException;

import javax.servlet.http.HttpServletResponse;

public enum Type {
    CODE("Code"),
    TOKEN("Token"),
    PASSWORD("Password");
    private final String value;

    Type(String value) {
        this.value = value;
    }

    public static Type forName(String value) {
        for (Type type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new HiveException("Illegal argument: " + value, HttpServletResponse.SC_BAD_REQUEST);

    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}

package com.devicehive.model.enums;

/**
 * Created by tmatvienko on 1/13/15.
 */
public enum AccessKeyType {
    DEFAULT(0),
    SESSION(1),
    OAUTH(2);

    private final int value;

    AccessKeyType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.name();
    }

    public static AccessKeyType forName(String value) {
        for (AccessKeyType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}

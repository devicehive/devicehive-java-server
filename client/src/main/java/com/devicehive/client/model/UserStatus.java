package com.devicehive.client.model;


public enum UserStatus {
    ACTIVE(0),
    LOCKED_OUT(1),
    DISABLED(2),
    DELETED(3);

    private int value;

    private UserStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}

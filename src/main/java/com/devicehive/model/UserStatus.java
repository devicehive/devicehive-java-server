package com.devicehive.model;

/**
 * Created with IntelliJ IDEA.
 * User: Anton
 * Date: 21.07.13
 * Time: 22:19
 * To change this template use File | Settings | File Templates.
 */
public enum UserStatus {
    ACTIVE(0),
    LOCKED_OUT(1),
    DISABLED(2),
    DELETED(3);

    private int value;

    UserStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}

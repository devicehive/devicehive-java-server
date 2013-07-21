package com.devicehive.model;

/**
 * Enum for User roles in the system.
 */
public enum UserRole {
    ADMIN(0),
    CLIENT(1);

    private final int value;

    UserRole(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.name();
    }
}

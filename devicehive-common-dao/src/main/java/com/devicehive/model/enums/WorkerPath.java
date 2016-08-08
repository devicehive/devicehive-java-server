package com.devicehive.model.enums;

/**
 * Created by tmatvienko on 3/3/15.
 */
public enum WorkerPath {
    NOTIFICATIONS("/notifications"), COMMANDS("/commands");

    private String value;

    private WorkerPath(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

package com.devicehive.websockets.util;

public enum HiveEndpoint {
    CLIENT("client"),
    DEVICE("device");
    String value;

    private HiveEndpoint(String value) {
        this.value = value;
    }

    public static HiveEndpoint byValue(String endpoint) {
        if (endpoint.equalsIgnoreCase(HiveEndpoint.CLIENT.value))
            return HiveEndpoint.CLIENT;
        else if (endpoint.equalsIgnoreCase(HiveEndpoint.DEVICE.value))
            return HiveEndpoint.DEVICE;
        else {
            return null;
        }
    }
}
package com.devicehive.client.model;


/**
 * Grant access type. Available values: Online: Access is requested to a limited period of time Offline: Assess is
 * requested for an unlimited period of time For more details see <a href="http://tools.ietf.org/html/rfc6749">The OAuth
 * 2.0 Authorization Framework</a>
 */
public enum AccessType {
    ONLINE("Online"),
    OFFLINE("Offline");
    private final String value;

    AccessType(String value) {
        this.value = value;
    }

    public static AccessType forName(String value) {
        for (AccessType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Illegal access type: " + value);

    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.name();
    }
}

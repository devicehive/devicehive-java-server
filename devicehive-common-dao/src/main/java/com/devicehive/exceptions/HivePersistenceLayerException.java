package com.devicehive.exceptions;

public class HivePersistenceLayerException extends HiveException {
    public HivePersistenceLayerException(String message, Throwable cause) {
        super(message, cause);
    }

    public HivePersistenceLayerException(String message) {
        super(message);
    }

    public HivePersistenceLayerException(String message, Integer code) {
        super(message, code);
    }

    public HivePersistenceLayerException(String message, Throwable cause, Integer code) {
        super(message, cause, code);
    }
}

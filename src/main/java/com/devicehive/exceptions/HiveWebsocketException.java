package com.devicehive.exceptions;


public class HiveWebsocketException extends RuntimeException {

    public HiveWebsocketException(String message, Throwable cause) {
        super(message, cause);
    }

    public HiveWebsocketException(String message) {
        super(message);
    }
}

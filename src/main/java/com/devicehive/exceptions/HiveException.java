package com.devicehive.exceptions;


public class HiveException extends RuntimeException {

    public HiveException(String message, Throwable cause) {
        super(message, cause);
    }

    public HiveException(String message) {
        super(message);
    }

}

package com.devicehive.exceptions;


public class ExampleException extends Exception {

    private static final long serialVersionUID = 8271791929053535928L;

    public ExampleException(String message) {
        super(message);
    }

    public ExampleException(String message, Throwable cause) {
        super(message, cause);
    }
}

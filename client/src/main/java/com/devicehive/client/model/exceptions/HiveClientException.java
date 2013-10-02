package com.devicehive.client.model.exceptions;

public class HiveClientException extends HiveException{

    public HiveClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public HiveClientException(String message) {
        super(message);
    }

    public HiveClientException(String message, Integer code) {
        super(message, code);
    }

    public HiveClientException(String message, Throwable cause, Integer code) {
        super(message, cause, code);
    }
}

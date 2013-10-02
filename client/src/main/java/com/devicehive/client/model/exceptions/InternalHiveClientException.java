package com.devicehive.client.model.exceptions;


//Is it really necessary to have 2 kinds of client exceptions?
public class InternalHiveClientException extends HiveClientException{


    public InternalHiveClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalHiveClientException(String message) {
        super(message);
    }

    public InternalHiveClientException(String message, Integer code) {
        super(message, code);
    }

    public InternalHiveClientException(String message, Throwable cause, Integer code) {
        super(message, cause, code);
    }
}

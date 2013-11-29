package com.devicehive.client.model.exceptions;


/**
 * TODO
 */
public class InternalHiveClientException extends HiveClientException {

    private static final long serialVersionUID = -8333329700032097189L;

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

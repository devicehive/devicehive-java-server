package com.devicehive.client.model.exceptions;


/**
 * Unexpected exception. Should not occur. If occurred, that means that library is used with incompatible version of
 * device hive server API.
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
}

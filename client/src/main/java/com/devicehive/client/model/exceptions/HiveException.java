package com.devicehive.client.model.exceptions;


/**
 * Common exception is used for all exceptional events: server errors, client errors,
 * internal exceptional library events
 */
public class HiveException extends RuntimeException {
    private static final long serialVersionUID = 6413354755792688308L;
    private Integer code = null;

    public HiveException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public HiveException(String message) {
        this(message, null, null);
    }

    public HiveException(String message, Integer code) {
        this(message, null, code);
    }

    public HiveException(String message, Throwable cause, Integer code) {
        super(message, cause);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}


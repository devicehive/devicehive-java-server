package com.devicehive.client.model.exceptions;


public class HiveException extends RuntimeException {
    private static final long serialVersionUID = 6413354755792688308L;

    private Integer code = null;

    public Integer getCode() {
        return code;
    }

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
}


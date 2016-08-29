package com.devicehive.service.exception;

public class BackendException extends Exception {

    private int errorerrorCode;

    public BackendException(int errorCode) {
        this.errorerrorCode = errorCode;
    }

    public BackendException(String message, int errorCode) {
        super(message);
        this.errorerrorCode = errorCode;
    }

    public BackendException(String message, Throwable cause, int errorCode) {
        super(message, cause);
        this.errorerrorCode = errorCode;
    }

    public BackendException(Throwable cause, int errorCode) {
        super(cause);
        this.errorerrorCode = errorCode;
    }

    public BackendException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int errorCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorerrorCode = errorCode;
    }
}

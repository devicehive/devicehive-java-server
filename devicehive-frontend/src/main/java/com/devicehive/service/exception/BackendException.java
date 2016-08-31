package com.devicehive.service.exception;

public class BackendException extends Exception {

    private int errorCode;

    public BackendException(int errorCode) {
        this.errorCode = errorCode;
    }

    public BackendException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BackendException(String message, Throwable cause, int errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public BackendException(Throwable cause, int errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public BackendException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int errorCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}

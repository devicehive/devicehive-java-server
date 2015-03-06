package com.devicehive.exception;

import org.springframework.http.HttpStatus;

/**
 * Created by tatyana on 2/9/15.
 */
public class HiveException extends RuntimeException {

    private HttpStatus status;

    public HiveException(String message, Throwable cause) {
        this(message, cause, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public HiveException(String message) {
        this(message, null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public HiveException(String message, HttpStatus status) {
        this(message, null, status);
    }

    public HiveException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }
}

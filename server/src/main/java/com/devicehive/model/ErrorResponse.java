package com.devicehive.model;

/**
 * Implements response object for error requests
 */
public class ErrorResponse implements HiveEntity {


    private static final long serialVersionUID = 286844689037405279L;


    private Integer error = null;

    private String message = null;

    public Integer getError() {
        return error;
    }

    public ErrorResponse(Integer errorCode, String messageString) {
        error = errorCode;
        message = messageString;
    }

    public ErrorResponse(String messageString) {
        message = messageString;
    }

    public void setError(Integer error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

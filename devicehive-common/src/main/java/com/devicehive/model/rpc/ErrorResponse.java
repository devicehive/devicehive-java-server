package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

public class ErrorResponse extends Body {

    private String message;
    private Exception exception;

    public ErrorResponse(String message, Exception exception) {
        super(Action.ERROR_RESPONSE.name());
        this.message = message;
        this.exception = exception;
    }

    public ErrorResponse(String message) {
        this(message, null);
    }

    public ErrorResponse(Exception exception) {
        this(exception.getMessage(), exception);
    }

    public String getMessage() {
        return message;
    }

    public Exception getException() {
        return exception;
    }
}

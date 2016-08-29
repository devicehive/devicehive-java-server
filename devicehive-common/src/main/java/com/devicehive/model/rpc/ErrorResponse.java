package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

public class ErrorResponse extends Body {

    private String message;

    public ErrorResponse(String message) {
        super(Action.ERROR_RESPONSE.name());
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

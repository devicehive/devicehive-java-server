package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

public class EchoRequest extends Body {
    private String request;

    public EchoRequest(String request) {
        super(Action.ECHO_REQUEST.name());
        this.request = request;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }
}

package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

public class EchoResponse extends Body {

    private String response;

    public EchoResponse(String response) {
        super("echo_response");
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}

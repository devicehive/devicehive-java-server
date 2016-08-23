package com.devicehive.model.rpc;

import com.devicehive.shim.api.RequestBody;

public class EchoRequest extends RequestBody {
    private String request;

    public EchoRequest(String request) {
        super("echo");
        this.request = request;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }
}

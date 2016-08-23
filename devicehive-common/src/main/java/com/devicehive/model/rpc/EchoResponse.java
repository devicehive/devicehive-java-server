package com.devicehive.model.rpc;

import com.devicehive.shim.api.ResponseBody;

public class EchoResponse extends ResponseBody {

    private String response;

    public EchoResponse(String response) {
        super("echo");
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}

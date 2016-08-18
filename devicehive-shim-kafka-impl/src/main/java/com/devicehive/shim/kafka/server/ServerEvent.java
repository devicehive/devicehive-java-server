package com.devicehive.shim.kafka.server;

import com.devicehive.shim.api.Request;

public class ServerEvent {

    private Request request;

    public void set(Request request) {
        this.request = request;
    }

    public Request get() {
        return request;
    }
}

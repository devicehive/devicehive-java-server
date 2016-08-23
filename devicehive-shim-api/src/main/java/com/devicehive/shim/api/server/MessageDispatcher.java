package com.devicehive.shim.api.server;

import com.devicehive.shim.api.Response;

public interface MessageDispatcher {

    void send(String to, Response response);

}

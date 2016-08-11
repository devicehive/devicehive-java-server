package com.devicehive.shim.api.server;

import com.devicehive.shim.api.Request;

public interface RequestHandler {

    Object handle(Request request);

}

package com.devicehive.shim.api.server;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;

public interface RequestHandler {

    Response handle(Request request);

}

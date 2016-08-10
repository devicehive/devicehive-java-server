package com.devicehive.shim.api.server;

import com.devicehive.shim.api.Request;

public interface Listener {

    Object onMessage(Request request);

}

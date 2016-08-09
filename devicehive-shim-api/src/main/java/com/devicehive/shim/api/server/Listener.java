package com.devicehive.shim.api.server;

import com.devicehive.shim.api.client.Request;

public interface Listener {
    void onMessage(Request request);
}

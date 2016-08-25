package com.devicehive.handler;

import com.devicehive.model.rpc.SubscribeRequest;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;

public class SubscriptionRequestHandler implements RequestHandler {

    @Override
    public Response handle(Request request) {
        SubscribeRequest body = (SubscribeRequest) request.getBody();

        return null;
    }

}

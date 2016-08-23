package com.devicehive.handler;

import com.devicehive.model.rpc.EchoRequest;
import com.devicehive.model.rpc.EchoResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;

public class EchoRequestHandler implements RequestHandler {

    @Override
    public Response handle(Request request) {
        return Response.newBuilder()
                .withBody(new EchoResponse(((EchoRequest) request.getBody()).getRequest())) // simple echo
                .withCorrelationId(request.getCorrelationId())
                .withLast(true)
                .buildSuccess();
    }
}

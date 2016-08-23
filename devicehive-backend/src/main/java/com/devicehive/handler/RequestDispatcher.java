package com.devicehive.handler;

import com.devicehive.model.rpc.Action;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;

import java.util.Map;
import java.util.Optional;

public class RequestDispatcher implements RequestHandler {

    private final Map<Action, RequestHandler> handlerMap;

    public RequestDispatcher(Map<Action, RequestHandler> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    public Response handle(Request request) {
        return Optional.ofNullable(handlerMap.get(Action.valueOf(request.getBody().getAction())))
                .map(handler -> handler.handle(request))
                .orElseThrow(UnsupportedOperationException::new);
    }
}

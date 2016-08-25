package com.devicehive.handler;

import com.devicehive.model.rpc.Action;
import com.devicehive.model.rpc.ErrorResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class RequestDispatcher implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestDispatcher.class);

    private final Map<Action, RequestHandler> handlerMap;

    @Autowired
    public RequestDispatcher(@Value("#{requestHandlerMap}") Map<Action, RequestHandler> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Response handle(Request request) {
        final Action action = Action.valueOf(request.getBody().getAction());
        try {
            return Optional.ofNullable(handlerMap.get(action))
                    .map(handler -> handler.handle(request))
                    .orElseThrow(() -> new RuntimeException("Action '" + action + "' is not supported."));
        } catch (Exception e) {
            logger.error("Unable to handle request.", e);
            return Response.newBuilder()
                    .withErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .withBody(new ErrorResponse(e))
                    .withLast(true)
                    .buildFailed();
        }
    }
}

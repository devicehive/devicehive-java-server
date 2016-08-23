package com.devicehive.shim.kafka.fixture;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;

public class RequestHandlerWrapper implements RequestHandler {
    private RequestHandler delegate;

    @Override
    public Response handle(Request request) {
        if (delegate == null) {
            throw new IllegalStateException("Request handler wasn't initialized");
        }

        return delegate.handle(request);
    }

    public void setDelegate(RequestHandler delegate) {
        this.delegate = delegate;
    }
}

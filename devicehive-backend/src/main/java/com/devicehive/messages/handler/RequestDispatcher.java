package com.devicehive.messages.handler;

/*
 * #%L
 * DeviceHive Backend Logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.application.RequestHandlersMapper;
import com.devicehive.shim.api.Action;
import com.devicehive.model.rpc.ErrorResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("request-dispatcher")
public class RequestDispatcher implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestDispatcher.class);

    private final RequestHandlersMapper requestHandlersMapper;

    @Autowired
    @Lazy
    public RequestDispatcher(RequestHandlersMapper requestHandlersMapper) {
        this.requestHandlersMapper = requestHandlersMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Response handle(Request request) {
        final Action action = request.getBody().getAction();
        try {
            return Optional.ofNullable(requestHandlersMapper.requestHandlerMap().get(action))
                    .map(handler -> handler.handle(request))
                    .orElseThrow(() -> new RuntimeException("Action '" + action + "' is not supported."));
        } catch (Exception e) {
            logger.error("Unable to handle request.", e);
            return Response.newBuilder()
                    .withBody(new ErrorResponse(e.getMessage()))
                    .withLast(true)
                    .buildFailed(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}

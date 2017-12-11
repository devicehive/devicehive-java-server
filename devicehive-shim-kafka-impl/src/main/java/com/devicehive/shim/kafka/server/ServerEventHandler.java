package com.devicehive.shim.kafka.server;

/*
 * #%L
 * DeviceHive Shim Kafka Implementation
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

import com.devicehive.model.ServerEvent;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.MessageDispatcher;
import com.devicehive.shim.api.server.RequestHandler;
import com.lmax.disruptor.WorkHandler;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ServerEventHandler implements MessageDispatcher, WorkHandler<ServerEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ServerEventHandler.class);

    private RequestHandler requestHandler;
    private Producer<String, Response> responseProducer;

    public ServerEventHandler(RequestHandler requestHandler, Producer<String, Response> responseProducer) {
        this.requestHandler = requestHandler;
        this.responseProducer = responseProducer;
    }

    @Override
    public void onEvent(ServerEvent event) throws Exception {
        final Request request = event.get();
        final String replyTo = request.getReplyTo();

        Response response;

        switch (request.getType()) {
            case clientRequest:
                logger.debug("Client request received {}", request);
                response = handleClientRequest(request);
                break;
            case ping:
                logger.info("Ping request received from {}", replyTo);
                response = Response.newBuilder().buildSuccess();
                break;
            default:
                logger.warn("Unknown type of request received {} from client with topic {}, correlationId = {}",
                        request.getType(), replyTo, request.getCorrelationId());
                response = Response.newBuilder()
                        .buildFailed(404);
        }

        // set correlationId explicitly to prevent missing it in request
        response.setCorrelationId(request.getCorrelationId());
        send(replyTo, response);
    }

    private Response handleClientRequest(Request request) {
        Response response;
        try {
            response = Optional.ofNullable(requestHandler.handle(request))
                    .orElseThrow(() -> new NullPointerException("Response must not be null"));
        } catch (Exception e) {
            logger.error("Unexpected exception occurred during request handling (action='{}', handler='{}')",
                    request.getBody().getAction().name(), requestHandler.getClass().getCanonicalName(), e);

            response = Response.newBuilder()
                    .withLast(request.isSingleReplyExpected())
                    .buildFailed(500);
        }
        return response;
    }

    @Override
    public void send(String replyTo, Response response) {
        responseProducer.send(new ProducerRecord<>(replyTo, response.getCorrelationId(), response));
    }
}

package com.devicehive.proxy;

/*
 * #%L
 * DeviceHive Backend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import com.devicehive.api.HandlersMapper;
import com.devicehive.model.ServerEvent;
import com.devicehive.proxy.api.ProxyClient;
import com.devicehive.proxy.api.ProxyMessage;
import com.devicehive.proxy.api.ProxyMessageBuilder;
import com.devicehive.proxy.api.payload.NotificationCreatePayload;
import com.devicehive.proxy.client.WebSocketKafkaProxyClient;
import com.devicehive.proxy.config.WebSocketKafkaProxyConfig;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.google.gson.Gson;
import com.lmax.disruptor.WorkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("ws-kafka-proxy-backend")
public class ProxyServerEventHandler implements WorkHandler<ServerEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ProxyServerEventHandler.class);

    private final Gson gson;
    private final ProxyClient proxyClient;
    private final HandlersMapper requestHandlersMapper;

    @Autowired
    public ProxyServerEventHandler(Gson gson, WebSocketKafkaProxyConfig proxyConfig, HandlersMapper requestHandlersMapper) {
        this.gson = gson;
        this.requestHandlersMapper = requestHandlersMapper;
        WebSocketKafkaProxyClient webSocketKafkaProxyClient = new WebSocketKafkaProxyClient((message, client) -> {});
        webSocketKafkaProxyClient.setWebSocketKafkaProxyConfig(proxyConfig);
        this.proxyClient = webSocketKafkaProxyClient;
        this.proxyClient.start();
    }

    @Override
    public void onEvent(ServerEvent serverEvent) throws Exception {
        final Request request = serverEvent.get();
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
        ProxyMessage responseMessage = ProxyMessageBuilder.notification(new NotificationCreatePayload(replyTo, gson.toJson(response)));
        proxyClient.push(responseMessage);
    }

    private Response handleClientRequest(Request request) {
        Response response;
        final Action action = request.getBody().getAction();

        RequestHandler requestHandler = requestHandlersMapper.requestHandlerMap().get(action);
        if (requestHandler == null) {
            throw new RuntimeException("Action '" + action + "' is not supported.");
        }
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
}

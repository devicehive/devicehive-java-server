package com.devicehive.proxy;

/*
 * #%L
 * DeviceHive Frontend Logic
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

import com.devicehive.api.RequestResponseMatcher;
import com.devicehive.model.ServerEvent;
import com.devicehive.proxy.api.NotificationHandler;
import com.devicehive.proxy.api.ProxyClient;
import com.devicehive.proxy.api.ProxyMessageBuilder;
import com.devicehive.proxy.api.payload.NotificationCreatePayload;
import com.devicehive.proxy.api.payload.SubscribePayload;
import com.devicehive.proxy.api.payload.TopicsPayload;
import com.devicehive.proxy.client.WebSocketKafkaProxyClient;
import com.devicehive.proxy.config.WebSocketKafkaProxyConfig;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.RequestType;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.google.gson.Gson;
import com.lmax.disruptor.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class FrontendProxyClient implements RpcClient {
    private static final Logger logger = LoggerFactory.getLogger(FrontendProxyClient.class);

    private final String requestTopic;
    private final String replyToTopic;
    private final WebSocketKafkaProxyClient client;
    private final WebSocketKafkaProxyConfig proxyConfig;
    private final NotificationHandler notificationHandler;
    private final RequestResponseMatcher requestResponseMatcher;
    private final Gson gson;
    private final RingBuffer<ServerEvent> ringBuffer;

    public FrontendProxyClient(String requestTopic, String replyToTopic, WebSocketKafkaProxyConfig proxyConfig, NotificationHandler notificationHandler, RequestResponseMatcher requestResponseMatcher, Gson gson, RingBuffer<ServerEvent> ringBuffer) {
        this.requestTopic = requestTopic;
        this.replyToTopic = replyToTopic;
        this.proxyConfig = proxyConfig;
        this.notificationHandler = notificationHandler;
        this.requestResponseMatcher = requestResponseMatcher;
        this.gson = gson;
        this.ringBuffer = ringBuffer;
        this.client = new WebSocketKafkaProxyClient((message, client) -> {});
        client.setWebSocketKafkaProxyConfig(proxyConfig);
    }

    @Override
    public void call(Request request, Consumer<Response> callback) {
        requestResponseMatcher.addRequestCallback(request.getCorrelationId(), callback);
        logger.debug("Request callback added for request: {}, correlationId: {}", request.getBody(), request.getCorrelationId());

        ringBuffer.publishEvent((serverEvent, sequence, response) -> serverEvent.set(response), request);
    }

    @Override
    public void push(Request request) {
        if (request.getBody() == null) {
            throw new NullPointerException("Request body must not be null.");
        }
        request.setReplyTo(replyToTopic);

        client.push(ProxyMessageBuilder.notification(
                new NotificationCreatePayload(requestTopic, gson.toJson(request), request.getPartitionKey())));
    }

    @Override
    public void start() {
        client.start();
        client.push(ProxyMessageBuilder.create(new TopicsPayload(Arrays.asList(requestTopic, replyToTopic)))).join();

        UUID uuid = UUID.randomUUID();
        Executor executionPool = Executors.newFixedThreadPool(proxyConfig.getWorkerThreads());
        for (int i = 0; i < proxyConfig.getWorkerThreads(); i++) {
            executionPool.execute(() -> {
                WebSocketKafkaProxyClient client = new WebSocketKafkaProxyClient(notificationHandler);
                client.setWebSocketKafkaProxyConfig(proxyConfig);
                client.start();
                client.push(ProxyMessageBuilder.subscribe(new SubscribePayload(replyToTopic, uuid.toString()))).join();
            });
        }

        pingServer();
    }

    @Override
    public void shutdown() {
        client.shutdown();
    }

    private void pingServer() {
        Request request = Request.newBuilder().build();
        request.setReplyTo(replyToTopic);
        request.setType(RequestType.ping);
        boolean connected = false;
        int attempts = 10;
        for (int i = 0; i < attempts; i++) {
            logger.info("Ping WebSocket Proxy attempt {}", i);

            CompletableFuture<Response> pingFuture = new CompletableFuture<>();

            requestResponseMatcher.addRequestCallback(request.getCorrelationId(), pingFuture::complete);
            logger.debug("Request callback added for request: {}, correlationId: {}", request.getBody(), request.getCorrelationId());

            client.push(ProxyMessageBuilder.notification(
                    new NotificationCreatePayload(requestTopic, gson.toJson(request), request.getPartitionKey())));

            Response response = null;
            try {
                response = pingFuture.get(3000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Exception occured while trying to ping Backend Server ", e);
            } catch (TimeoutException e) {
                logger.warn("Backend Server didn't respond to ping request");
                continue;
            } finally {
                requestResponseMatcher.removeRequestCallback(request.getCorrelationId());
            }
            if (response != null && !response.isFailed()) {
                connected = true;
                break;
            } else {
                shutdown();
            }
        }
        if (connected) {
            logger.info("Successfully connected to Backend Server");
        } else {
            logger.error("Unable to reach out Backend Server in {} attempts", attempts);
            throw new RuntimeException("Backend Server is not reachable");
        }
    }
}

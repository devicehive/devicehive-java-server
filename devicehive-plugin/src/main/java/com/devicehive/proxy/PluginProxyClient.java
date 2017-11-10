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

import com.devicehive.proxy.api.ProxyClient;
import com.devicehive.proxy.api.ProxyMessageBuilder;
import com.devicehive.proxy.api.payload.NotificationCreatePayload;
import com.devicehive.proxy.api.payload.TopicCreatePayload;
import com.devicehive.proxy.api.payload.TopicSubscribePayload;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.RequestType;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.kafka.client.RequestResponseMatcher;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class PluginProxyClient implements RpcClient {
    private static final Logger logger = LoggerFactory.getLogger(PluginProxyClient.class);

    private final String requestTopic;
    private final String replyToTopic;
    private final ProxyClient client;
    private final RequestResponseMatcher requestResponseMatcher;
    private final Gson gson;

    public PluginProxyClient(String requestTopic, String replyToTopic, ProxyClient client, RequestResponseMatcher requestResponseMatcher, Gson gson) {
        this.requestTopic = requestTopic;
        this.replyToTopic = replyToTopic;
        this.client = client;
        this.requestResponseMatcher = requestResponseMatcher;
        this.gson = gson;
    }

    @Override
    public void call(Request request, Consumer<Response> callback) {
        requestResponseMatcher.addRequestCallback(request.getCorrelationId(), callback);
        logger.debug("Request callback added for request: {}, correlationId: {}", request.getBody(), request.getCorrelationId());

        push(request);
    }

    @Override
    public void push(Request request) {
        if (request.getBody() == null) {
            throw new NullPointerException("Request body must not be null.");
        }
        request.setReplyTo(replyToTopic);

        client.push(ProxyMessageBuilder.notification(new NotificationCreatePayload(requestTopic, gson.toJson(request)))); // toDo: use request partition key
    }
    
    public void createTopic(List<String> topics) {
        client.push(ProxyMessageBuilder.create(new TopicCreatePayload(topics))).join();
    }
    
    public void subscribeToTopic(String topic) {
        client.push(ProxyMessageBuilder.subscribe(new TopicSubscribePayload(topic))).join();
    }
    
    @Override
    public void start() {
        client.start();
        createTopic(Arrays.asList(requestTopic, replyToTopic));
        subscribeToTopic(replyToTopic);
        
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
            logger.info("Ping RpcServer attempt {}", i);

            CompletableFuture<Response> pingFuture = new CompletableFuture<>();

            requestResponseMatcher.addRequestCallback(request.getCorrelationId(), pingFuture::complete);
            logger.debug("Request callback added for request: {}, correlationId: {}", request.getBody(), request.getCorrelationId());

            client.push(ProxyMessageBuilder.notification(new NotificationCreatePayload(requestTopic, gson.toJson(request)))); // toDo: use request partition key

            Response response = null;
            try {
                response = pingFuture.get(3000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Exception occured while trying to ping RpcServer ", e);
            } catch (TimeoutException e) {
                logger.warn("RpcServer didn't respond to ping request");
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
            logger.info("Successfully connected to RpcServer");
        } else {
            logger.error("Unable to reach out RpcServer in {} attempts", attempts);
            throw new RuntimeException("RpcServer is not reachable");
        }
    }
}

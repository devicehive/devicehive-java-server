package com.devicehive.shim.kafka.client;

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

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.RequestType;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class KafkaRpcClient implements RpcClient {
    private static final Logger logger = LoggerFactory.getLogger(KafkaRpcClient.class);

    private String requestTopic;
    private String replyToTopic;
    private Producer<String, Request> requestProducer;
    private RequestResponseMatcher requestResponseMatcher;
    private ServerResponseListener responseListener;

    public KafkaRpcClient(String requestTopic, String replyToTopic, Producer<String, Request> requestProducer,
                          RequestResponseMatcher requestResponseMatcher, ServerResponseListener responseListener) {
        this.requestTopic = requestTopic;
        this.replyToTopic = replyToTopic;
        this.requestProducer = requestProducer;
        this.requestResponseMatcher = requestResponseMatcher;
        this.responseListener = responseListener;
    }

    @Override
    public void start() {
        responseListener.startWorkers();
        pingServer();
    }

    @Override
    public void call(Request request, Consumer<Response> callback) {
        push(request);
        requestResponseMatcher.addRequestCallback(request.getCorrelationId(), callback);
    }

    @Override
    public void push(Request request) {
        if (request.getBody() == null) {
            throw new NullPointerException("Request body must not be null.");
        }

        request.setReplyTo(replyToTopic);
        requestProducer.send(new ProducerRecord<>(requestTopic, request.getPartitionKey(), request),
                (recordMetadata, e) -> {
                    if (e != null) {
                        logger.error("Send request failed", e);
                    }
                    logger.debug("Request {} sent successfully", request.getCorrelationId());
                    //TODO [rafa] in case sending fails - we need to notify the caller using the callback passed.
                });
    }

    @Override
    public boolean ping() {
        Request request = Request.newBuilder().build();
        request.setReplyTo(replyToTopic);
        request.setType(RequestType.ping);

        logger.debug("Ping RpcServer");

        CompletableFuture<Response> pingFuture = new CompletableFuture<>();

        requestResponseMatcher.addRequestCallback(request.getCorrelationId(), pingFuture::complete);
        requestProducer.send(new ProducerRecord<>(requestTopic, request.getPartitionKey(), request));

        Response response = null;
        try {
            response = pingFuture.get(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception occured while trying to ping RpcServer ", e);
        } catch (TimeoutException e) {
            logger.warn("RpcServer didn't respond to ping request");
        } finally {
            requestResponseMatcher.removeRequestCallback(request.getCorrelationId());
        }

        return response != null && !response.isFailed();
    }

    @Override
    public void shutdown() {
        requestProducer.close();
        responseListener.shutdown();
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
            requestProducer.send(new ProducerRecord<>(requestTopic, request.getPartitionKey(), request));

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
                responseListener.shutdown();
                responseListener.startWorkers();
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

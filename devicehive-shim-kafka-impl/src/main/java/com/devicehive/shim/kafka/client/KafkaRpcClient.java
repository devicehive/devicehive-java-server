package com.devicehive.shim.kafka.client;

import com.devicehive.shim.KafkaRpcConfig;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class KafkaRpcClient implements RpcClient {
    private static final Logger logger = LoggerFactory.getLogger(KafkaRpcClient.class);

    private Producer<String, Request> requestProducer;
    private ResponseSupplier responseSupplier;

    public KafkaRpcClient(Producer<String, Request> requestProducer, ResponseSupplier responseSupplier) {
        this.requestProducer = requestProducer;
        this.responseSupplier = responseSupplier;
    }

    @Override
    public CompletableFuture<Response> call(Request request) {
        push(request);
        CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        responseSupplier.putRequest(request.getCorrelationId(), responseFuture);
        return responseFuture;
    }

    @Override
    public void push(Request request) {
        requestProducer.send(new ProducerRecord<>(KafkaRpcConfig.REQUEST_TOPIC, request.getCorrelationId(), request),
                (recordMetadata, e) -> {
                    if (e != null) {
                        logger.error("Send request failed", e);
                    }
                    logger.trace("Request {} sent successfully", request.getCorrelationId());
                });
    }


}

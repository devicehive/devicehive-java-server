package com.devicehive.shim.kafka.client;

import com.devicehive.shim.api.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RpcClientResponseSupplier {
    private static final Logger logger = LoggerFactory.getLogger(RpcClientResponseSupplier.class);

    private final ConcurrentHashMap<String, CompletableFuture<Response>> correlationMap = new ConcurrentHashMap<>();

    void putRequest(String correlationId, CompletableFuture<Response> future) {
        correlationMap.put(correlationId, future);
    }

    void offerResponse(Response response) {
        CompletableFuture<Response> future = correlationMap.get(response.getCorrelationId());
        try {
            if (future != null && !future.isDone()) {
                future.complete(response);
            } else {
                logger.warn("Unexpected response received {}", response);
            }
        } finally {
            correlationMap.remove(response.getCorrelationId());
        }
    }

}

package com.devicehive.shim.kafka.client;

import com.devicehive.shim.api.Response;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RequestResponseMatcher {
    private final ConcurrentHashMap<String, Consumer<Response>> correlationMap = new ConcurrentHashMap<>();

    void addRequestCallback(String correlationId, Consumer<Response> callback) {
        correlationMap.put(correlationId, callback);
    }

    void offerResponse(Response response) {
        Consumer<Response> callback = correlationMap.get(response.getCorrelationId());
        if (callback != null) {
            try {
                callback.accept(response);
            } finally {
                if (response.isLast()) {
                    correlationMap.remove(response.getCorrelationId());
                }
            }
        }
    }

}

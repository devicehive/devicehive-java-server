package com.devicehive.shim.kafka.client;

import com.devicehive.shim.api.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RequestResponseMatcher {
    private static final Logger logger = LoggerFactory.getLogger(RequestResponseMatcher.class);

    private final ConcurrentHashMap<String, Consumer<Response>> correlationMap = new ConcurrentHashMap<>();

    void addRequestCallack(String correlationId, Consumer<Response> callbackFunc) {
        correlationMap.put(correlationId, callbackFunc);
    }

    void offerResponse(Response response) {
        Consumer<Response> callback = correlationMap.get(response.getCorrelationId());
        try {
            callback.accept(response);
        } finally {
            if (response.isLast()) {
                correlationMap.remove(response.getCorrelationId());
            }
        }
    }

}

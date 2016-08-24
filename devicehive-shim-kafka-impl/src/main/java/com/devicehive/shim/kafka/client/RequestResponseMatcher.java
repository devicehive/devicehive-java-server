package com.devicehive.shim.kafka.client;

import com.devicehive.shim.api.Response;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

public class RequestResponseMatcher {

    private final ConcurrentHashMap<String, Consumer<Response>> correlationMap = new ConcurrentHashMap<>();

    //TODO [rafa] we do not really need FJP, but rather some other pool implementation. Though FJP looks good, it might be over kill for our use case.
    private final ForkJoinPool executionPool = new ForkJoinPool();

    void addRequestCallback(String correlationId, Consumer<Response> callback) {
        correlationMap.put(correlationId, callback);
    }

    void offerResponse(Response response) {
        Consumer<Response> callback = correlationMap.get(response.getCorrelationId());
        if (callback != null) {
            executionPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        callback.accept(response);
                    } finally {
                        if (response.isLast()) {
                            correlationMap.remove(response.getCorrelationId());
                        }
                    }
                }
            });
        }
    }

}

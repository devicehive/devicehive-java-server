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

import com.devicehive.shim.api.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

public class RequestResponseMatcher {
    private static final Logger logger = LoggerFactory.getLogger(RequestResponseMatcher.class);

    private final ConcurrentHashMap<String, Consumer<Response>> correlationMap = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> subscriptionMap = new HashMap<>();

    //TODO [rafa] we do not really need FJP, but rather some other pool implementation. Though FJP looks good, it might be over kill for our use case.
    private final ForkJoinPool executionPool = new ForkJoinPool();

    public void addRequestCallback(String correlationId, Consumer<Response> callback) {
        correlationMap.put(correlationId, callback);
    }

    public void removeRequestCallback(String correlationId) {
        correlationMap.remove(correlationId);
    }

    public void offerResponse(Response response) {
        Consumer<Response> callback = correlationMap.get(response.getCorrelationId());
        if (callback != null) {
            executionPool.execute(() -> {
                try {
                    callback.accept(response);
                } finally {
                    if (response.isLast()) {
                        correlationMap.remove(response.getCorrelationId());
                    }
                }
            });
        } else {
            logger.warn("Callback was not found for {}. Map size: {}, response: {}", response.getCorrelationId(), correlationMap.size(), response.getBody());
        }
    }

    public void addSubscription(Long subscriptionId, String correlationId) {
        if (subscriptionMap.containsKey(subscriptionId)) {
            Set<String> corrIds = subscriptionMap.get(subscriptionId);
            corrIds.add(correlationId);
        } else {
            Set<String> set = new HashSet<>();
            set.add(correlationId);
            subscriptionMap.put(subscriptionId, set);
        }
    }

    public void removeSubscription(Long subscriptionId) {
        if (subscriptionMap.containsKey(subscriptionId)) {
            subscriptionMap.get(subscriptionId).forEach(correlationMap::remove);
            subscriptionMap.remove(subscriptionId);
        }
    }
}

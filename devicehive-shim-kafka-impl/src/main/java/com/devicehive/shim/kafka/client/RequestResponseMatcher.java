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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

@Component
@PropertySource("classpath:application.properties")
public class RequestResponseMatcher {
    private static final Logger logger = LoggerFactory.getLogger(RequestResponseMatcher.class);

    @Value("${request.response.matcher.threads:8}")
    private int threadsCount;

    private final ConcurrentHashMap<String, Consumer<Response>> correlationMap = new ConcurrentHashMap<>();

    private ForkJoinPool executionPool;

    @PostConstruct
    public void init() {
        logger.info("ForkJoinPool size: {}", threadsCount);
        executionPool = new ForkJoinPool(threadsCount);
    }

    void addRequestCallback(String correlationId, Consumer<Response> callback) {
        correlationMap.put(correlationId, callback);
    }

    void removeRequestCallback(String correlationId) {
        correlationMap.remove(correlationId);
    }

    void offerResponse(Response response) {
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

}

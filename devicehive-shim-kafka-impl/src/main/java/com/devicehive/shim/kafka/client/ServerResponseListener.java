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

import com.devicehive.api.RequestResponseMatcher;
import com.devicehive.shim.api.Response;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerResponseListener {
    private static final Logger logger = LoggerFactory.getLogger(ServerResponseListener.class);

    private String topic;
    private int consumerThreads;
    private RequestResponseMatcher requestResponseMatcher;
    private Properties consumerProps;
    private ExecutorService consumerExecutor;
    private Deserializer<Response> deserializer;

    private List<ResponseConsumerWorker> workers;

    public ServerResponseListener(String topic, int consumerThreads, RequestResponseMatcher requestResponseMatcher,
                                  Properties consumerProps, ExecutorService consumerExecutor, Deserializer<Response> deserializer) {
        this.topic = topic;
        this.consumerThreads = consumerThreads;
        this.requestResponseMatcher = requestResponseMatcher;
        this.consumerProps = consumerProps;
        this.consumerExecutor = consumerExecutor;
        this.deserializer = deserializer;
    }

    public void startWorkers() {
        CountDownLatch latch = new CountDownLatch(consumerThreads);
        workers = new ArrayList<>(consumerThreads);
        for (int i = 0; i < consumerThreads; i++) {
            KafkaConsumer<String, Response> consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(), deserializer);
            ResponseConsumerWorker worker = new ResponseConsumerWorker(topic, requestResponseMatcher, consumer, latch);
            consumerExecutor.submit(worker);
            workers.add(worker);
        }
        try {
            latch.await();
            logger.info("RpcClient response consumers started");
        } catch (InterruptedException e) {
            logger.error("Error while waiting for client consumers to subscribe", e);
        }
    }

    public void shutdown() {
        workers.forEach(ResponseConsumerWorker::shutdown);
        consumerExecutor.shutdown();
        try {
            consumerExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Exception occurred while shutting executor service", e);
        }
    }
}

package com.devicehive.shim.kafka.server;

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

import com.devicehive.model.ServerEvent;
import com.devicehive.shim.api.Request;
import com.lmax.disruptor.RingBuffer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RequestConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RequestConsumer.class);

    private String topic;
    private Properties consumerProps;
    private int consumerThreads;
    private Deserializer<Request> deserializer;

    private ExecutorService consumerExecutor;
    private List<RequestConsumerWorker> workers;

    public RequestConsumer(String topic, Properties consumerProps, int consumerThreads, Deserializer<Request> deserializer) {
        this.topic = topic;
        this.consumerProps = consumerProps;
        this.consumerThreads = consumerThreads;
        this.deserializer = deserializer;
    }

    public void startConsumers(RingBuffer<ServerEvent> ringBuffer) {
        assert ringBuffer != null;

        workers = new ArrayList<>(consumerThreads);
        consumerExecutor = Executors.newFixedThreadPool(consumerThreads);
        CountDownLatch latch = new CountDownLatch(consumerThreads);
        for (int i = 0; i < consumerThreads; i++) {
            KafkaConsumer<String, Request> consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(), deserializer);
            RequestConsumerWorker worker = new RequestConsumerWorker(this.topic, consumer, ringBuffer, latch);
            consumerExecutor.submit(worker);
            workers.add(worker);
        }
        try {
            latch.await();
            logger.info("RpcServer request consumers started");
        } catch (InterruptedException e) {
            logger.error("Error while waiting for consumers", e);
        }
    }

    public void shutdownConsumers() {
        workers.forEach(RequestConsumerWorker::shutdown);
        consumerExecutor.shutdown();
        try {
            consumerExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Error while waiting for server consumers to subscribe", e);
        }
    }
}

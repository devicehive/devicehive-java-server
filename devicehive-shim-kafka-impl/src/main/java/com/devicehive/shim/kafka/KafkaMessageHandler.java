package com.devicehive.shim.kafka;

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

import com.devicehive.shim.config.KafkaRpcConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;


public abstract class KafkaMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageHandler.class);

    private final String topic;
    private final KafkaRpcConfig kafkaRpcConfig;
    private final Producer<String, String> producer;
    private final ForkJoinPool executionPool;

    public KafkaMessageHandler(KafkaRpcConfig kafkaRpcConfig, String topic) {
        this.topic = topic;
        this.kafkaRpcConfig = kafkaRpcConfig;
        this.producer = new KafkaProducer<>(kafkaRpcConfig.producerProps(), new StringSerializer(), new StringSerializer());
        this.executionPool = new ForkJoinPool();
    }

    public void start() {
        int consumerThreads = kafkaRpcConfig.getHandlerThreads();
        Properties properties = kafkaRpcConfig.clientConsumerProps();
        CountDownLatch latch = new CountDownLatch(consumerThreads);

        for (int i = 0; i < consumerThreads; i++) {
            KafkaConsumer<String, String> consumer =
                    new KafkaConsumer<>(properties, new StringDeserializer(), new StringDeserializer());
            executionPool.execute(new ConsumerWorker<String>(topic, consumer, latch) {
                @Override
                public void process(ConsumerRecord<String, String> record) {
                    handle(record.value());
                }
            });
        }

        try {
            latch.await();
            logger.info("Kafka message handler has started");
        } catch (InterruptedException e) {
            logger.error("Error while waiting for consumers", e);
        }
    }

    public void shutdown() {
        executionPool.shutdown();
        logger.info("Kafka message handler has stopped");
    }

    public abstract void handle(String message);

    public void push(String message) {
        logger.debug("Pushed message to the topic {}", topic);
        producer.send(new ProducerRecord<>(topic, message));
    }
}

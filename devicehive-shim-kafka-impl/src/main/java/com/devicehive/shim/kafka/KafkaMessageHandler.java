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

import java.util.concurrent.CountDownLatch;


public abstract class KafkaMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageHandler.class);

    private final String topic;
    private final Producer<String, String> producer;
    private final ConsumerWorker worker;
    private final Thread workerThread;

    public KafkaMessageHandler(KafkaRpcConfig kafkaRpcConfig, String topic) {
        this.topic = topic;
        this.producer = new KafkaProducer<>(kafkaRpcConfig.producerProps(), new StringSerializer(), new StringSerializer());
        KafkaConsumer<String, String> consumer =
                new KafkaConsumer<>(kafkaRpcConfig.clientConsumerProps(), new StringDeserializer(), new StringDeserializer());

        this.worker = new ConsumerWorker<String>(topic, consumer, new CountDownLatch(1)) {
            @Override
            public void process(ConsumerRecord<String, String> record) {
                handle(record.value());
            }
        };
        this.workerThread = new Thread(worker, "KafkaMessageHandlerThread");
    }

    public void start() {
        logger.info("Kafka message handler has started");
        workerThread.start();
    }

    public void shutdown() {
        logger.info("Kafka message handler has stopped");
        worker.shutdown();
    }

    public abstract void handle(String message);

    public void push(String message) {
        logger.debug("Pushed message to the topic {}", topic);
        producer.send(new ProducerRecord<>(topic, message));
    }
}

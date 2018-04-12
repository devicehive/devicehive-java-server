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


import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.internals.NoOpConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public abstract class ConsumerWorker<T> implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerWorker.class);

    private String topic;
    private KafkaConsumer<String, T> consumer;
    private CountDownLatch latch;

    public ConsumerWorker(String topic, KafkaConsumer<String, T> consumer, CountDownLatch latch) {
        this.topic = topic;
        this.consumer = consumer;
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(Collections.singletonList(topic), new NoOpConsumerRebalanceListener() {
                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    latch.countDown();
                }
            });
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, T> records = consumer.poll(Long.MAX_VALUE);
                records.forEach(record -> {
                    logger.trace("Topic {}, partition {}, offset {}", record.topic(), record.partition(), record.offset());
                    process(record);
                });
            }
        }  catch (WakeupException e) {
            logger.warn("Kafka consumer thread is shutting down");
        } catch (Exception e) {
            logger.error("Unexpected exception in server Kafka consumer", e);
        } finally {
            consumer.close();
        }
    }

    public abstract void process(ConsumerRecord<String, T> record);

    public void shutdown() {
        consumer.wakeup();
    }
}

package com.devicehive.application.kafka;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

import com.devicehive.messages.kafka.IConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * Author: Yuliia Vovk
 * Date: 25.02.16
 * Time: 15:34
 */
public class ConsumerWorkable<T> implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerWorkable.class);
    private final KafkaConsumer<String, T> consumer;
    private final String topic;
    private final IConsumer<T> provider;

    public ConsumerWorkable(KafkaConsumer<String, T> consumer,
                            String topic, IConsumer<T> provider) {
        this.topic = topic;
        this.consumer = consumer;
        this.provider = provider;
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(Collections.singletonList(topic));

            while (!Thread.currentThread().isInterrupted()) {
                final ConsumerRecords<String, T> records = consumer.poll(Long.MAX_VALUE);
                for (ConsumerRecord<String, T> record : records) {
                    final T message = record.value();
                    LOGGER.debug("Topic {}, partition {}, message {} ", topic, record.partition(), message);
                    provider.submitMessage(message);
                }
            }
        } catch (WakeupException e) {
            LOGGER.debug("Consuming thread is shutting down");
        } finally {
            consumer.close();
        }
    }

    public void shutdown() {
        consumer.wakeup();
    }
}

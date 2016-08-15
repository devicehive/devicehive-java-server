package com.devicehive.shim.kafka.server;

import com.devicehive.shim.api.Request;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public class RequestConsumerWorker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RequestConsumerWorker.class);

    private String topic;
    private KafkaConsumer<String, Request> consumer;
    private KafkaMessageDispatcher messageDispatcher;
    private CountDownLatch startupLatch;

    public RequestConsumerWorker(String topic, KafkaConsumer<String, Request> consumer,
                                 KafkaMessageDispatcher requestDispatcher, CountDownLatch startupLatch) {
        this.topic = topic;
        this.consumer = consumer;
        this.messageDispatcher = requestDispatcher;
        this.startupLatch = startupLatch;
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(Collections.singletonList(topic));
            startupLatch.countDown();
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, Request> records = consumer.poll(Long.MAX_VALUE);
                records.forEach(record -> {
                    logger.trace("Topic {}, partition {}, offset {}", record.topic(), record.partition(), record.offset());
                    messageDispatcher.onReceive(record.value());
                });
            }

        } catch (WakeupException e) {
            logger.warn("Response Consumer thread is shutting down");
        } finally {
            consumer.close();
        }
    }

    public void shutdown() {
        consumer.wakeup();
    }

}

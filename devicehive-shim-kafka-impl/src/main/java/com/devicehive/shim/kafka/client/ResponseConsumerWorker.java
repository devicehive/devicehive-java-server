package com.devicehive.shim.kafka.client;

import com.devicehive.shim.api.Response;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class ResponseConsumerWorker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ResponseConsumerWorker.class);

    private String topic;
    private RpcClientResponseSupplier responseSupplier;
    private KafkaConsumer<String, Response> consumer;

    public ResponseConsumerWorker(String topic, RpcClientResponseSupplier responseSupplier, KafkaConsumer<String, Response> consumer) {
        this.topic = topic;
        this.responseSupplier = responseSupplier;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(Collections.singletonList(topic));

            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, Response> records = consumer.poll(Long.MAX_VALUE);
                records.forEach(record -> {
                    logger.trace("Topic {}, partition {}, offset {}", record.topic(), record.partition(), record.offset());
                    responseSupplier.offerResponse(record.value());
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

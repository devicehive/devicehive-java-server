package com.devicehive.shim.kafka.server;

import com.devicehive.shim.kafka.client.KafkaRpcClientConfig;
import com.devicehive.shim.api.Request;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class RequestConsumerWorker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RequestConsumerWorker.class);

    private KafkaConsumer<String, Request> consumer;
    private ClientRequestHandler requestHandler;

    public RequestConsumerWorker(KafkaConsumer<String, Request> consumer, ClientRequestHandler requestHandler) {
        this.consumer = consumer;
        this.requestHandler = requestHandler;
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(Collections.singletonList(KafkaRpcClientConfig.REQUEST_TOPIC));

            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, Request> records = consumer.poll(Long.MAX_VALUE);
                records.forEach(record -> {
                    logger.trace("Topic {}, partition {}, offset {}", record.topic(), record.partition(), record.offset());
                    requestHandler.handleRequest(record.value());
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

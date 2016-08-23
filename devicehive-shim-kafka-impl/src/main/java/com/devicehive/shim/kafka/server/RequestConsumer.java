package com.devicehive.shim.kafka.server;

import com.devicehive.shim.api.Request;
import com.lmax.disruptor.RingBuffer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
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
        for (int i = 0; i < consumerThreads; i++) {
            KafkaConsumer<String, Request> consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(), deserializer);
            RequestConsumerWorker worker = new RequestConsumerWorker(this.topic, consumer, ringBuffer);
            consumerExecutor.submit(worker);
            workers.add(worker);
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

    public static class RequestConsumerWorker implements Runnable {

        private String topic;
        private KafkaConsumer<String, Request> consumer;
        private RingBuffer<ServerEvent> ringBuffer;

        RequestConsumerWorker(String topic, KafkaConsumer<String, Request> consumer, RingBuffer<ServerEvent> ringBuffer) {
            this.topic = topic;
            this.consumer = consumer;
            this.ringBuffer = ringBuffer;
        }

        @Override
        public void run() {
            try {
                consumer.subscribe(Collections.singleton(topic));

                while (!Thread.currentThread().isInterrupted()) {
                    ConsumerRecords<String, Request> records = consumer.poll(Long.MAX_VALUE);
                    records.forEach(record -> {
                        logger.trace("Topic {}, partition {}, offset {}", record.topic(), record.partition(), record.offset());
                        ringBuffer.publishEvent((serverEvent, sequence, response) -> serverEvent.set(response), record.value());
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
}

package com.devicehive.shim.kafka.client;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KafkaRpcClient implements RpcClient {
    private static final Logger logger = LoggerFactory.getLogger(KafkaRpcClient.class);

    private String replyToTopic;
    private Properties requestProducerProps;
    private Properties responseConsumerProps;
    private int consumerThreads;

    private Producer<String, Request> requestProducer;
    private RpcClientResponseSupplier responseSupplier;

    private ExecutorService executor;
    private List<ResponseConsumerWorker> workers;

    private KafkaRpcClient(String replyToTopic, Properties requestProducerProps, Properties responseConsumerProps, int consumerThreads) {
        this.replyToTopic = replyToTopic;
        this.requestProducerProps = requestProducerProps;
        this.responseConsumerProps = responseConsumerProps;
        this.consumerThreads = consumerThreads;

        start();
    }

    private void start() {
        requestProducer = new KafkaProducer<>(requestProducerProps);
        responseSupplier = new RpcClientResponseSupplier();

        executor = Executors.newFixedThreadPool(consumerThreads);
        workers = new ArrayList<>(consumerThreads);
        for (int i = 0; i < consumerThreads; i++) {
            KafkaConsumer<String, Response> consumer = new KafkaConsumer<>(responseConsumerProps);
            ResponseConsumerWorker worker = new ResponseConsumerWorker(replyToTopic, responseSupplier, consumer);
            workers.add(worker);
            executor.submit(worker);
        }
    }

    @Override
    public void shutdown() {
        requestProducer.close();
        workers.forEach(ResponseConsumerWorker::shutdown);
        executor.shutdown();
        try {
            executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Exception occurred while shutting executor service: {}", e);
        }
    }

    @Override
    public CompletableFuture<Response> call(Request request) {
        request.setReplyTo(replyToTopic);
        push(request);
        CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        responseSupplier.putRequest(request.getCorrelationId(), responseFuture);
        return responseFuture;
    }

    @Override
    public void push(Request request) {
        requestProducer.send(new ProducerRecord<>(KafkaRpcClientConfig.REQUEST_TOPIC, request.getCorrelationId(), request),
                (recordMetadata, e) -> {
                    if (e != null) {
                        logger.error("Send request failed", e);
                    }
                    logger.debug("Request {} sent successfully", request.getCorrelationId());
                });
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String replyToTopic;
        private Properties requestProducerProps;
        private Properties responseConsumerProps;
        private int consumerThreads;

        public Builder withReplyTo(String replyToTopic) {
            this.replyToTopic = replyToTopic;
            return this;
        }

        public Builder withRequestProducerProps(Properties requestProducerProps) {
            this.requestProducerProps = requestProducerProps;
            return this;
        }

        public Builder withResponseConsumerProps(Properties responseConsumerProps) {
            this.responseConsumerProps = responseConsumerProps;
            return this;
        }

        public Builder withConsumerThreads(int consumerThreads) {
            this.consumerThreads = consumerThreads;
            return this;
        }

        public KafkaRpcClient build() {
            return new KafkaRpcClient(replyToTopic, requestProducerProps, responseConsumerProps, consumerThreads);
        }
    }

}

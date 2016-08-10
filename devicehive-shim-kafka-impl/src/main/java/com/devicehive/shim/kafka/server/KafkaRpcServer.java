package com.devicehive.shim.kafka.server;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.Listener;
import com.devicehive.shim.api.server.RpcServer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KafkaRpcServer implements RpcServer {

    private final int consumerThreads;
    private final int workerThreads;
    private final Listener listener;
    private final Properties consumerProps;
    private final Properties producerProps;

    private List<RequestConsumerWorker> consumerWorkers;
    private ClientRequestHandler requestHandler;

    private KafkaRpcServer(int consumerThreads, int workerThreads, Listener listener, Properties consumerProps, Properties producerProps) {
        this.consumerThreads = consumerThreads;
        this.workerThreads = workerThreads;
        this.listener = listener;
        this.consumerProps = consumerProps;
        this.producerProps = producerProps;

        consumerWorkers = new ArrayList<>(consumerThreads);
    }

    @Override
    public void start() {
        ExecutorService consumerExecutor = Executors.newFixedThreadPool(consumerThreads);
        ExecutorService workerExecutor = Executors.newFixedThreadPool(workerThreads);

        Producer<String, Response> responseProducer = new KafkaProducer<>(producerProps);
        requestHandler = new ClientRequestHandler(listener, workerExecutor, responseProducer);

        for (int i = 0; i < consumerThreads; i++) {
            KafkaConsumer<String, Request> consumer = new KafkaConsumer<>(consumerProps);
            RequestConsumerWorker worker = new RequestConsumerWorker(consumer, requestHandler);
            consumerWorkers.add(worker);
            consumerExecutor.submit(worker);
        }
    }

    @Override
    public void shutdown() {
        requestHandler.shutdown();
        consumerWorkers.forEach(RequestConsumerWorker::shutdown);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Properties consumerProps;
        private Properties producerProps;
        private Listener listener;
        private int consumerThreads;
        private int workerThreads;

        public Builder withRequestConsumerProps(Properties consumerProps) {
            this.consumerProps = consumerProps;
            return this;
        }

        public Builder withResponseProducerProps(Properties producerProps) {
            this.producerProps = producerProps;
            return this;
        }

        public Builder withListener(Listener listener) {
            this.listener = listener;
            return this;
        }

        public Builder withConsumerThreads(int consumerThreads) {
            this.consumerThreads = consumerThreads;
            return this;
        }

        public Builder withWorkerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
            return this;
        }

        public KafkaRpcServer build() {
            return new KafkaRpcServer(consumerThreads, workerThreads, listener, consumerProps, producerProps);
        }
    }
}

package com.devicehive.shim.kafka.builder;

import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.shim.api.server.RpcServer;
import com.devicehive.shim.kafka.server.ClientRequestDispatcher;
import com.devicehive.shim.kafka.server.KafkaRpcServer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerBuilder {

    private String topic;

    private int consumerThreads;
    private int workerThreads;

    private Properties consumerProps;
    private Properties producerProps;

    private RequestHandler requestHandler;

    public ServerBuilder withTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public ServerBuilder withConsumerThreads(int consumerThreads) {
        this.consumerThreads = consumerThreads;
        return this;
    }

    public ServerBuilder withWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
        return this;
    }

    public ServerBuilder withProducerProps(Properties producerProps) {
        this.producerProps = producerProps;
        return this;
    }

    public ServerBuilder withConsumerProps(Properties consumerProps) {
        this.consumerProps = consumerProps;
        return this;
    }

    public ServerBuilder withRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
        return this;
    }

    public RpcServer build() {
        ExecutorService workerExecutor = Executors.newFixedThreadPool(workerThreads);
        Producer<String, Response> responseProducer = new KafkaProducer<>(producerProps);
        ClientRequestDispatcher requestDispatcher = new ClientRequestDispatcher(requestHandler, workerExecutor, responseProducer);

        ExecutorService consumerExecutor = Executors.newFixedThreadPool(consumerThreads);
        return new KafkaRpcServer(topic, consumerThreads, consumerProps, consumerExecutor, requestDispatcher);
    }

}

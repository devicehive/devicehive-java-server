package com.devicehive.shim.kafka.builder;

import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.shim.api.server.RpcServer;
import com.devicehive.shim.kafka.server.KafkaRpcServer;
import com.devicehive.shim.kafka.server.RequestConsumer;
import com.devicehive.shim.kafka.server.ServerEvent;
import com.devicehive.shim.kafka.server.ServerEventHandler;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
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
        ProducerType producerType = ProducerType.SINGLE;
        if (consumerThreads > 1) {
            producerType = ProducerType.MULTI;
        }
        ExecutorService workerExecutor = Executors.newFixedThreadPool(workerThreads);
        Disruptor<ServerEvent> disruptor = new Disruptor<>(ServerEvent::new, 1024, workerExecutor, producerType, new BlockingWaitStrategy());

        Producer<String, Response> responseProducer = new KafkaProducer<>(producerProps);
        ServerEventHandler eventHandler = new ServerEventHandler(requestHandler, responseProducer);

        RequestConsumer requestConsumer = new RequestConsumer(topic, consumerProps, consumerThreads);
        return new KafkaRpcServer(disruptor, requestConsumer, eventHandler);
    }

}

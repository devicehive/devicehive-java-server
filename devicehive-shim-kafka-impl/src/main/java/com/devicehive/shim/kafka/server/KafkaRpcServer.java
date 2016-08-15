package com.devicehive.shim.kafka.server;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.server.MessageDispatcher;
import com.devicehive.shim.api.server.RpcServer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class KafkaRpcServer implements RpcServer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaRpcServer.class);

    private String topic;
    private int consumerThreads;
    private Properties consumerProps;
    private ExecutorService consumerExecutor;
    private KafkaMessageDispatcher messageDispatcher;

    private List<RequestConsumerWorker> consumerWorkers;

    public KafkaRpcServer(String topic, int consumerThreads, Properties consumerProps,
                          ExecutorService consumerExecutor, KafkaMessageDispatcher messageDispatcher) {
        this.topic = topic;
        this.consumerThreads = consumerThreads;
        this.consumerProps = consumerProps;
        this.consumerExecutor = consumerExecutor;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void start() {
        consumerWorkers = new ArrayList<>(consumerThreads);
        CountDownLatch startupLatch = new CountDownLatch(consumerThreads);
        for (int i = 0; i < consumerThreads; i++) {
            KafkaConsumer<String, Request> consumer = new KafkaConsumer<>(consumerProps);
            RequestConsumerWorker worker = new RequestConsumerWorker(topic, consumer, messageDispatcher, startupLatch);
            consumerExecutor.submit(worker);
        }

        try {
            startupLatch.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Error while waiting for server consumers to subscribe", e);
        }
    }

    @Override
    public void shutdown() {
        messageDispatcher.shutdown();
        consumerWorkers.forEach(RequestConsumerWorker::shutdown);
    }

    @Override
    public MessageDispatcher getDispatcher() {
        return messageDispatcher;
    }
}

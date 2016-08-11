package com.devicehive.shim.kafka.client;

import com.devicehive.shim.api.Response;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerResponseListener {
    private static final Logger logger = LoggerFactory.getLogger(ServerResponseListener.class);

    private String topic;
    private RequestResponseMatcher requestResponseMatcher;
    private Properties consumerProps;
    private ExecutorService consumerExecutor;

    private List<ResponseConsumerWorker> workers;

    public ServerResponseListener(String topic, RequestResponseMatcher requestResponseMatcher, Properties consumerProps, ExecutorService consumerExecutor) {
        this.topic = topic;
        this.requestResponseMatcher = requestResponseMatcher;
        this.consumerProps = consumerProps;
        this.consumerExecutor = consumerExecutor;
    }

    public void startWorkers(int count) {
        workers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            KafkaConsumer<String, Response> consumer = new KafkaConsumer<>(consumerProps);
            ResponseConsumerWorker worker = new ResponseConsumerWorker(topic, requestResponseMatcher, consumer);
            consumerExecutor.submit(worker);
        }
    }

    public void shutdown() {
        workers.forEach(ResponseConsumerWorker::shutdown);
        consumerExecutor.shutdown();
        try {
            consumerExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Exception occurred while shutting executor service: {}", e);
        }
    }
}

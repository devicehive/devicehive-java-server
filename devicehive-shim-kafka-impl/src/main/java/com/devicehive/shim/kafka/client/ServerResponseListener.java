package com.devicehive.shim.kafka.client;

import com.devicehive.shim.api.Response;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

public class ServerResponseListener {

    private String topic;
    private RequestResponseMatcher requestResponseMatcher;
    private Properties consumerProps;
    private ExecutorService consumerExecutor;

    public ServerResponseListener(String topic, RequestResponseMatcher requestResponseMatcher, Properties consumerProps, ExecutorService consumerExecutor) {
        this.topic = topic;
        this.requestResponseMatcher = requestResponseMatcher;
        this.consumerProps = consumerProps;
        this.consumerExecutor = consumerExecutor;
    }

    public List<ResponseConsumerWorker> startWorkers(int count) {
        List<ResponseConsumerWorker> workers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            KafkaConsumer<String, Response> consumer = new KafkaConsumer<>(consumerProps);
            ResponseConsumerWorker worker = new ResponseConsumerWorker(topic, requestResponseMatcher, consumer);
            consumerExecutor.submit(worker);
        }
        return workers;
    }
}

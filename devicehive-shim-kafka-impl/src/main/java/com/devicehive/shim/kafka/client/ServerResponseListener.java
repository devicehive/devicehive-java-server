package com.devicehive.shim.kafka.client;

import com.devicehive.shim.api.Response;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import com.google.gson.Gson;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerResponseListener {
    private static final Logger logger = LoggerFactory.getLogger(ServerResponseListener.class);

    private String topic;
    private int consumerThreads;
    private RequestResponseMatcher requestResponseMatcher;
    private Properties consumerProps;
    private ExecutorService consumerExecutor;
    private Deserializer<Response> deserializer;

    private List<ResponseConsumerWorker> workers;

    public ServerResponseListener(String topic, int consumerThreads, RequestResponseMatcher requestResponseMatcher,
                                  Properties consumerProps, ExecutorService consumerExecutor, Deserializer<Response> deserializer) {
        this.topic = topic;
        this.consumerThreads = consumerThreads;
        this.requestResponseMatcher = requestResponseMatcher;
        this.consumerProps = consumerProps;
        this.consumerExecutor = consumerExecutor;
        this.deserializer = deserializer;
    }

    public void startWorkers() {
        CountDownLatch startupLatch = new CountDownLatch(consumerThreads);
        workers = new ArrayList<>(consumerThreads);
        for (int i = 0; i < consumerThreads; i++) {
            KafkaConsumer<String, Response> consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(), deserializer);
            ResponseConsumerWorker worker = new ResponseConsumerWorker(topic, requestResponseMatcher, consumer, startupLatch);
            consumerExecutor.submit(worker);
            workers.add(worker);
        }
        try {
            startupLatch.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Error while waiting for client consumers to subscribe", e);
        }
    }

    public void shutdown() {
        workers.forEach(ResponseConsumerWorker::shutdown);
        consumerExecutor.shutdown();
        try {
            consumerExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Exception occurred while shutting executor service", e);
        }
    }
}

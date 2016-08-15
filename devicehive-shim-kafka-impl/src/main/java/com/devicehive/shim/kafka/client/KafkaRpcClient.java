package com.devicehive.shim.kafka.client;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class KafkaRpcClient implements RpcClient {
    private static final Logger logger = LoggerFactory.getLogger(KafkaRpcClient.class);

    private String requestTopic;
    private String replyToTopic;
    private Producer<String, Request> requestProducer;
    private RequestResponseMatcher requestResponseMatcher;
    private ServerResponseListener responseListener;

    public KafkaRpcClient(String requestTopic, String replyToTopic, Producer<String, Request> requestProducer,
                          RequestResponseMatcher requestResponseMatcher, ServerResponseListener responseListener) {
        this.requestTopic = requestTopic;
        this.replyToTopic = replyToTopic;
        this.requestProducer = requestProducer;
        this.requestResponseMatcher = requestResponseMatcher;
        this.responseListener = responseListener;
    }

    @Override
    public void start() {
        responseListener.startWorkers();
    }

    @Override
    public void call(Request request, Consumer<Response> callback) {
        push(request);
        requestResponseMatcher.addRequestCallback(request.getCorrelationId(), callback);
    }

    @Override
    public void push(Request request) {
        request.setReplyTo(replyToTopic);
        requestProducer.send(new ProducerRecord<>(requestTopic, request.getCorrelationId(), request),
                (recordMetadata, e) -> {
                    if (e != null) {
                        logger.error("Send request failed", e);
                    }
                    logger.debug("Request {} sent successfully", request.getCorrelationId());
                });
    }

    @Override
    public void shutdown() {
        requestProducer.close();
        responseListener.shutdown();
    }
}

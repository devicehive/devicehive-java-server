package com.devicehive.shim.kafka.client;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class KafkaRpcClient implements RpcClient {
    private static final Logger logger = LoggerFactory.getLogger(KafkaRpcClient.class);

    private String requestTopic;
    private String replyToTopic;
    private Producer<String, Request> requestProducer;
    private RequestResponseMatcher requestResponseMatcher;

    public KafkaRpcClient(String requestTopic, String replyToTopic, Producer<String, Request> requestProducer, RequestResponseMatcher requestResponseMatcher) {
        this.requestTopic = requestTopic;
        this.replyToTopic = replyToTopic;
        this.requestProducer = requestProducer;
        this.requestResponseMatcher = requestResponseMatcher;
    }

    @Override
    public CompletableFuture<Response> call(Request request) {
        request.setReplyTo(replyToTopic);
        push(request);
        CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        requestResponseMatcher.putRequest(request.getCorrelationId(), responseFuture);
        return responseFuture;
    }

    @Override
    public void push(Request request) {
        requestProducer.send(new ProducerRecord<>(requestTopic, request.getCorrelationId(), request),
                (recordMetadata, e) -> {
                    if (e != null) {
                        logger.error("Send request failed", e);
                    }
                    logger.debug("Request {} sent successfully", request.getCorrelationId());
                });
    }

}

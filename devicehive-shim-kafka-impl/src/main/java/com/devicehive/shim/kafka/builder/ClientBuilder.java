package com.devicehive.shim.kafka.builder;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.kafka.client.KafkaRpcClient;
import com.devicehive.shim.kafka.client.RequestResponseMatcher;
import com.devicehive.shim.kafka.client.ServerResponseListener;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientBuilder {

    private String requestTopic;
    private String replyTopic;

    private Properties producerProps;
    private Serializer<Request> producerValueSerializer;

    private Properties consumerProps;
    private Deserializer<Response> consumerValueDeserializer;


    private int consumerThreads;

    public ClientBuilder withRequestTopic(String requestTopic) {
        this.requestTopic = requestTopic;
        return this;
    }

    public ClientBuilder withReplyTopic(String replyTopic) {
        this.replyTopic = replyTopic;
        return this;
    }

    public ClientBuilder withProducerProps(Properties producerProps) {
        this.producerProps = producerProps;
        return this;
    }

    public ClientBuilder withConsumerProps(Properties consumerProps) {
        this.consumerProps = consumerProps;
        return this;
    }

    public ClientBuilder withConsumerThreads(int consumerThreads) {
        this.consumerThreads = consumerThreads;
        return this;
    }

    public ClientBuilder withProducerValueSerializer(Serializer<Request> serializer) {
        this.producerValueSerializer = serializer;
        return this;
    }

    public ClientBuilder withConsumerValueDeserializer(Deserializer<Response> deserializer) {
        this.consumerValueDeserializer = deserializer;
        return this;
    }

    public RpcClient build() {
        RequestResponseMatcher matcher = new RequestResponseMatcher();

        ExecutorService consumerExecutor = Executors.newFixedThreadPool(consumerThreads);
        ServerResponseListener responseListener = new ServerResponseListener(replyTopic, consumerThreads,
                matcher, consumerProps, consumerExecutor, consumerValueDeserializer);

        Producer<String, Request> requestProducer = new KafkaProducer<>(producerProps, new StringSerializer(), producerValueSerializer);
        return new KafkaRpcClient(requestTopic, replyTopic, requestProducer, matcher, responseListener);
    }

}

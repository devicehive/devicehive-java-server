package com.devicehive.shim.kafka.test;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.shim.api.server.RpcServer;
import com.devicehive.shim.kafka.builder.ClientBuilder;
import com.devicehive.shim.kafka.builder.ServerBuilder;
import com.devicehive.shim.kafka.rule.KafkaEmbeddedRule;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class KafkaRpcClientServerCommunicationTest {

    private static final String REQUEST_TOPIC = "request_topic";
    private static final String RESPONSE_TOPIC = "response_topic";

    @ClassRule
    public static KafkaEmbeddedRule kafkaRule = new KafkaEmbeddedRule(true, 1, REQUEST_TOPIC, RESPONSE_TOPIC);

    private Properties serverConsumerProps;
    private Properties serverProducerProps;

    private Properties clientProducerProps;
    private Properties clientConsumerProps;

    @Before
    public void setUp() throws Exception {
        serverConsumerProps = kafkaRule.getConsumerProperties();
        serverConsumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, RequestSerializer.class.getName());
        serverProducerProps = kafkaRule.getProducerProperties();
        serverProducerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ResponseSerializer.class.getName());

        clientProducerProps = kafkaRule.getProducerProperties();
        clientProducerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, RequestSerializer.class.getName());
        clientConsumerProps = kafkaRule.getConsumerProperties();
        clientConsumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ResponseSerializer.class.getName());
    }

    private RpcServer buildServer(RequestHandler requestHandler) {
        return new ServerBuilder()
                .withConsumerProps(serverConsumerProps)
                .withProducerProps(serverProducerProps)
                .withConsumerThreads(1)
                .withWorkerThreads(1)
                .withRequestHandler(requestHandler)
                .withTopic(REQUEST_TOPIC)
                .build();
    }

    private RpcClient buildClient() {
        return new ClientBuilder()
                .withProducerProps(clientProducerProps)
                .withConsumerProps(clientConsumerProps)
                .withReplyTopic(RESPONSE_TOPIC)
                .withRequestTopic(REQUEST_TOPIC)
                .withConsumerThreads(1)
                .build();
    }

    @Test
    public void shouldSendRequestToServer() throws Exception {
        CompletableFuture<Request> future = new CompletableFuture<>();
        RequestHandler mockedHandler = request -> {
            future.complete(request);
            return Response.newBuilder()
                    .withCorrelationId(request.getCorrelationId())
                    .withBody("Response".getBytes())
                    .withLast(true)
                    .buildSuccess();
        };

        RpcServer server = buildServer(mockedHandler);
        server.start();

        RpcClient client = buildClient();
        client.start();

        Request request = Request.newBuilder()
                .withCorrelationId(UUID.randomUUID().toString())
                .withSingleReply(true)
                .withBody("RequestResponseTest".getBytes())
                .build();

        TimeUnit.SECONDS.sleep(10);

        client.push(request);

        Request receivedRequest = future.get(20, TimeUnit.SECONDS);
        assertEquals(request, receivedRequest);
    }

}

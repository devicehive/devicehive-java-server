package com.devicehive.shim.kafka.test;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.rule.KafkaEmbeddedRule;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.api.server.RpcServer;
import com.devicehive.shim.kafka.builder.ClientBuilder;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.devicehive.shim.config.client.KafkaRpcClientConfig.RESPONSE_TOPIC;
import static com.devicehive.shim.config.server.KafkaRpcServerConfig.REQUEST_TOPIC;
import static org.junit.Assert.*;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@IntegrationTest
@SpringApplicationConfiguration(classes = {DeviceHiveApplication.class})
public class RpcServerConfigTest {

    @ClassRule
    public static KafkaEmbeddedRule kafkaRule = new KafkaEmbeddedRule(true, 1, REQUEST_TOPIC, RESPONSE_TOPIC);

    @Autowired
    private RpcServer server;

    private static RpcClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        Properties clientProducerProps = kafkaRule.getProducerProperties();
        clientProducerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, RequestSerializer.class.getName());
        Properties clientConsumerProps = kafkaRule.getConsumerProperties();
        clientConsumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ResponseSerializer.class.getName());

        client = new ClientBuilder()
                .withProducerProps(clientProducerProps)
                .withConsumerProps(clientConsumerProps)
                .withReplyTopic(RESPONSE_TOPIC)
                .withRequestTopic(REQUEST_TOPIC)
                .withConsumerThreads(1)
                .build();
        client.start();
        TimeUnit.SECONDS.sleep(10);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Optional.ofNullable(client).ifPresent(RpcClient::shutdown);
    }

    @Test
    public void shouldSuccessfullyReplyToRequest() throws Exception {
        final String testBody = "RequestResponseTest" + System.currentTimeMillis();
        Request request = Request.newBuilder()
                .withCorrelationId(UUID.randomUUID().toString())
                .withSingleReply(true)
                .withBody(testBody.getBytes())
                .build();

        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        assertNotNull(response);
        assertEquals(request.getCorrelationId(), response.getCorrelationId());
        assertEquals(testBody, new String(response.getBody()));
        assertTrue(response.isLast());
        assertFalse(response.isFailed());
    }
}

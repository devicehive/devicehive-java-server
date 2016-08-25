package com.devicehive.handler.notification;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.json.GsonFactory;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.rpc.NotificationInsertRequest;
import com.devicehive.rule.KafkaEmbeddedRule;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.kafka.builder.ClientBuilder;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import com.google.gson.Gson;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.devicehive.shim.config.client.KafkaRpcClientConfig.RESPONSE_TOPIC;
import static com.devicehive.shim.config.server.KafkaRpcServerConfig.REQUEST_TOPIC;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@IntegrationTest
@SpringApplicationConfiguration(classes = {DeviceHiveApplication.class})
public class NotificationInsertHandlerTest {

    @ClassRule
    public static KafkaEmbeddedRule kafkaRule = new KafkaEmbeddedRule(true, 1, REQUEST_TOPIC, RESPONSE_TOPIC);

    private static RpcClient client;

    private static Gson gson = GsonFactory.createGson();

    @Autowired
    private HazelcastService hazelcastService;

    @BeforeClass
    public static void setUp() throws Exception {
        client = new ClientBuilder()
                .withProducerProps(kafkaRule.getProducerProperties())
                .withConsumerProps(kafkaRule.getConsumerProperties())
                .withConsumerValueDeserializer(new ResponseSerializer(gson))
                .withProducerValueSerializer(new RequestSerializer(gson))
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
    public void testInsertNotification() throws ExecutionException, InterruptedException {
        final String corelationId = UUID.randomUUID().toString();
        final String guid = UUID.randomUUID().toString();
        final long id = System.nanoTime();

        DeviceNotification originalNotification = new DeviceNotification();
        originalNotification.setTimestamp(Date.from(Instant.now()));
        originalNotification.setId(id);
        originalNotification.setDeviceGuid(guid);
        originalNotification.setNotification("SOME TEST DATA");
        originalNotification.setParameters(new JsonStringWrapper("{\"param1\":\"value1\",\"param2\":\"value2\"}"));

        final CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(Request.newBuilder()
                .withCorrelationId(corelationId)
                .withBody(new NotificationInsertRequest(originalNotification))
                .withPartitionKey(originalNotification.getDeviceGuid())
                .build(), future::complete);

        Response response = future.get();
        Assert.assertEquals(corelationId, response.getCorrelationId());
        Assert.assertTrue(hazelcastService.find(id, guid, DeviceNotification.class)
                .filter(notification -> notification.equals(originalNotification))
                .isPresent());
    }
}

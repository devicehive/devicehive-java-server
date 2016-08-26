package com.devicehive.handler.notification;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.rpc.NotificationInsertRequest;
import com.devicehive.rule.KafkaEmbeddedRule;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.devicehive.shim.config.client.KafkaRpcClientConfig.RESPONSE_TOPIC;
import static com.devicehive.shim.config.server.KafkaRpcServerConfig.REQUEST_TOPIC;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@IntegrationTest
@SpringApplicationConfiguration(classes = {DeviceHiveApplication.class})
public class NotificationInsertHandlerTest {

    @ClassRule
    public static KafkaEmbeddedRule kafkaRule = new KafkaEmbeddedRule(true, 1, REQUEST_TOPIC, RESPONSE_TOPIC);

    @Autowired
    private RpcClient client;

    @Autowired
    private HazelcastService hazelcastService;

    @Test
    public void testInsertNotification() throws ExecutionException, InterruptedException, TimeoutException {
        // FIXME: HACK! We must find a better solution to postpone test execution until all components (shim, kafka, etc) will be ready
        TimeUnit.SECONDS.sleep(10);

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

        Response response = future.get(10, TimeUnit.SECONDS);
        Assert.assertEquals(corelationId, response.getCorrelationId());
        Assert.assertTrue(hazelcastService.find(id, guid, DeviceNotification.class)
                .filter(notification -> notification.equals(originalNotification))
                .isPresent());
    }
}

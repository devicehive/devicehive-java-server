package com.devicehive.shim.kafka.test;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.model.rpc.EchoRequest;
import com.devicehive.model.rpc.EchoResponse;
import com.devicehive.rule.KafkaEmbeddedRule;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
    private RpcClient client;

    @Test
    public void shouldSuccessfullyReplyToRequest() throws Exception {
        // FIXME: HACK! We must find a better solution to postpone test execution until all components (shim, kafka, etc) will be ready
        TimeUnit.SECONDS.sleep(10);

        final String testBody = "RequestResponseTest" + System.currentTimeMillis();
        Request request = Request.newBuilder()
                .withBody(new EchoRequest(testBody))
                .withCorrelationId(UUID.randomUUID().toString())
                .withSingleReply(true)
                .build();

        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        @SuppressWarnings("unchecked")
        Response response = future.get(10, TimeUnit.SECONDS);
        assertNotNull(response);
        assertEquals(request.getCorrelationId(), response.getCorrelationId());
        assertEquals(testBody, ((EchoResponse) response.getBody()).getResponse());
        assertTrue(response.isLast());
        assertFalse(response.isFailed());
    }
}

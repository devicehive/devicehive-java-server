package com.devicehive.shim.kafka.test;

import com.devicehive.base.AbstractSpringTest;
import com.devicehive.model.rpc.EchoRequest;
import com.devicehive.model.rpc.EchoResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class RpcServerConfigTest extends AbstractSpringTest {

    @Autowired
    private RpcClient client;

    @Test
    public void shouldSuccessfullyReplyToRequest() throws Exception {
        final String testBody = "RequestResponseTest" + System.currentTimeMillis();
        Request request = Request.newBuilder()
                .withBody(new EchoRequest(testBody))
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

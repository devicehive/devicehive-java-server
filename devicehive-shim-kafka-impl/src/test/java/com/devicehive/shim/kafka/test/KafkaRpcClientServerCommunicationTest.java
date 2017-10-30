package com.devicehive.shim.kafka.test;

/*
 * #%L
 * DeviceHive Shim Kafka Implementation
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.json.adapters.RuntimeTypeAdapterFactory;
import com.devicehive.shim.api.Action;
import com.devicehive.test.rule.KafkaEmbeddedRule;
import com.devicehive.shim.api.Body;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.shim.api.server.RpcServer;
import com.devicehive.shim.kafka.builder.ClientBuilder;
import com.devicehive.shim.kafka.builder.ServerBuilder;
import com.devicehive.shim.kafka.fixture.RequestHandlerWrapper;
import com.devicehive.shim.kafka.fixture.TestRequestBody;
import com.devicehive.shim.kafka.fixture.TestResponseBody;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.*;
import org.junit.rules.Timeout;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class KafkaRpcClientServerCommunicationTest {

    private static final String REQUEST_TOPIC = "request_topic";
    private static final String RESPONSE_TOPIC = "response_topic";

    @ClassRule
    public static KafkaEmbeddedRule kafkaRule = new KafkaEmbeddedRule(true, 1, REQUEST_TOPIC, RESPONSE_TOPIC);

    @Rule
    public Timeout testTimeout = new Timeout(180000, TimeUnit.MILLISECONDS); // 180k ms = 3 minutes

    private static RpcServer server;
    private static RpcClient client;

    private static RequestHandlerWrapper handlerWrapper = new RequestHandlerWrapper();

    private static Gson gson;

    @BeforeClass
    public static void setUp() throws Exception {
        RuntimeTypeAdapterFactory<Body> requestFactory = RuntimeTypeAdapterFactory.of(Body.class, "a")
                .registerSubtype(TestResponseBody.class, 0)
                .registerSubtype(TestRequestBody.class, 1);

        gson = new GsonBuilder()
                .registerTypeAdapterFactory(requestFactory)
                .create();

        server = new ServerBuilder()
                .withConsumerProps(kafkaRule.getConsumerProperties())
                .withProducerProps(kafkaRule.getProducerProperties())
                .withConsumerValueDeserializer(new RequestSerializer(gson))
                .withProducerValueSerializer(new ResponseSerializer(gson))
                .withConsumerThreads(1)
                .withRequestHandler(handlerWrapper)
                .withTopic(REQUEST_TOPIC)
                .build();
        server.start();

        client = new ClientBuilder()
                .withProducerProps(kafkaRule.getProducerProperties())
                .withConsumerProps(kafkaRule.getConsumerProperties())
                .withProducerValueSerializer(new RequestSerializer(gson))
                .withConsumerValueDeserializer(new ResponseSerializer(gson))
                .withReplyTopic(RESPONSE_TOPIC)
                .withRequestTopic(REQUEST_TOPIC)
                .withConsumerThreads(1)
                .build();
        client.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (client != null) {
            client.shutdown();
        }
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void shouldSendRequestToServer() throws Exception {
        CompletableFuture<Request> future = new CompletableFuture<>();
        RequestHandler handler = request -> {
            future.complete(request);
            return Response.newBuilder()
                    .withBody(new TestResponseBody("Response"))
                    .withCorrelationId(request.getCorrelationId())
                    .withLast(true)
                    .buildSuccess();
        };
        handlerWrapper.setDelegate(handler);

        Request request = Request.newBuilder()
                .withBody(new TestRequestBody("RequestResponseTest"))
                .withSingleReply(true)
                .build();

        client.push(request);

        Request receivedRequest = future.get(10, TimeUnit.SECONDS);
        assertEquals(request, receivedRequest);
    }

    @Test
    public void shouldSuccessfullyReplyToRequest() throws Exception {
        RequestHandler handler = request -> Response.newBuilder()
                .withBody(new TestResponseBody("ResponseFromServer"))
                .withCorrelationId(request.getCorrelationId())
                .withLast(true)
                .buildSuccess();
        handlerWrapper.setDelegate(handler);

        Request request = Request.newBuilder()
                .withBody(new TestRequestBody("RequestResponseTest"))
                .withSingleReply(true)
                .build();

        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        assertNotNull(response);
        assertEquals(request.getCorrelationId(), response.getCorrelationId());
        assertTrue(response.getBody() instanceof TestResponseBody);
        assertEquals("ResponseFromServer", ((TestResponseBody) response.getBody()).getResponseBody());
        assertTrue(response.isLast());
        assertFalse(response.isFailed());
    }

    @Test
    public void shouldSendErrorToClient() throws Exception {
        RequestHandler handler = request -> {
            throw new RuntimeException("Something went wrong");
        };
        handlerWrapper.setDelegate(handler);

        Request request = Request.newBuilder()
                .withBody(new TestRequestBody("RequestResponseTest"))
                .withSingleReply(true)
                .build();

        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        assertNotNull(response);
        assertEquals(request.getCorrelationId(), response.getCorrelationId());
        assertTrue(response.isLast());
        assertTrue(response.isFailed());
        assertNull(response.getBody());
    }

    @Test
    public void shouldSendMultipleResponsesToClient() throws Exception {
        RequestHandler handler = request -> Response.newBuilder()
                .withBody(new TestResponseBody("ResponseFromServer"))
                .withCorrelationId(request.getCorrelationId())
                .withLast(request.isSingleReplyExpected())
                .buildSuccess();
        handlerWrapper.setDelegate(handler);

        Request request = Request.newBuilder()
                .withBody(new TestRequestBody("RequestResponseTest"))
                .withSingleReply(false)
                .build();

        CountDownLatch latch = new CountDownLatch(10);
        List<Response> responses = Collections.synchronizedList(new LinkedList<>());
        Consumer<Response> func = response -> {
            responses.add(response);
            latch.countDown();
        };

        client.call(request, func);

        Executor executor = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 9; i++) {
            final int number = i;
            executor.execute(() -> {
                Response response = Response.newBuilder()
                        .withBody(new TestResponseBody(number + "-response"))
                        .withCorrelationId(request.getCorrelationId())
                        .withLast(false)
                        .buildSuccess();
                server.getDispatcher().send(RESPONSE_TOPIC, response);
            });
        }

        latch.await();
        assertEquals(10, responses.size());

        Set<String> correlationIds = responses.stream()
                .map(Response::getCorrelationId).collect(Collectors.toSet());
        assertEquals(1, correlationIds.size());
        assertTrue(correlationIds.contains(request.getCorrelationId()));

        Set<String> bodies = responses.stream()
                .map(Response::getBody)
                .map(responseBody -> (TestResponseBody) responseBody)
                .map(TestResponseBody::getResponseBody)
                .collect(Collectors.toSet());
        assertEquals(10, bodies.size());
        assertTrue(bodies.contains("ResponseFromServer"));
        for (int i = 0; i < 9; i++) {
            assertTrue(bodies.contains(i + "-response"));
        }
    }

}

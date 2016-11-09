package com.devicehive.websockets;

/*
 * #%L
 * DeviceHive Frontend Logic
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

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.fixture.JsonFixture;
import com.google.gson.JsonObject;
import org.junit.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * This is the only test that should work with web sockets directly.
 * This is the only class that may send websocket messages.<br/>
 * <p>
 * Other websocket functionality verification tests should work through the injection and message passing via java calls.
 */
public class WebSocketApiInfoHandlerTest extends AbstractResourceTest {

    @Test
    public void shouldReturnApiInfo() throws Exception {
        final String requestId = "62345vxgsa5";

        CompletableFuture<TextMessage> future = new CompletableFuture<>();
        new StandardWebSocketClient()
                .doHandshake(new TextWebSocketHandler() {
                    @Override
                    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                        future.complete(message);
                    }
                }, wsBaseUri() + "/websocket/client")
                .addCallback(session -> {
                    JsonObject apiInfoRequest = JsonFixture.createWsCommand("server/info", requestId);
                    try {
                        session.sendMessage(new TextMessage(gson.toJson(apiInfoRequest)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, future::completeExceptionally);

        future.thenAccept(response -> {
            JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);

            assertThat(jsonResp.get("action").getAsString(), is("server/info"));
            assertThat(jsonResp.get("requestId").getAsString(), is(requestId));
            assertThat(jsonResp.get("status").getAsString(), is("success"));
        }).exceptionally(e -> {
            fail(e.getMessage());
            return null;
        }).get(5, TimeUnit.SECONDS);
    }
}

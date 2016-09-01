package com.devicehive.websockets;

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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * This is the only test that should work with web sockets directly.
 * This is the only class that may send websocket messages.<br/>
 * <p>
 * Other websocket functionality verification tests should work through the injection and message passing via java calls.
 */
public class WebsocketApiInfoHandler extends AbstractResourceTest {

    @Test(timeout = 5 * 1000) // 5 seconds
    public void shouldReturnApiInfo() {
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
        }).join();
    }
}

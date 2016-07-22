package com.devicehive.websockets;

import com.devicehive.base.AbstractWebSocketTest;
import com.devicehive.base.fixture.JsonFixture;
import com.devicehive.base.fixture.WebSocketFixture;
import com.devicehive.base.websocket.WebSocketSynchronousConnection;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Test;
import org.springframework.web.socket.TextMessage;

import static com.devicehive.base.websocket.WebSocketSynchronousConnection.WAIT_TIMEOUT;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * This is the only test that should work with web sockets directly.
 * This is the only class that may send websocket messages.<br/>
 *
 * Other websocket functionality verification tests should work through the injection and message passing via java calls.
 */
public class WebsocketApiInfoHandler extends AbstractWebSocketTest {

    @After
    public void tearDown() throws Exception {
        clearWSConnections();
    }

    @Test
    public void there_should_return_api_info() throws Exception {
        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");
        WebSocketFixture.authenticateUser(ADMIN_LOGIN, ADMIN_PASS, connection);

        String requestId = "62345vxgsa5";

        JsonObject apiInfoRequest = JsonFixture.createWsCommand("server/info", requestId);
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(apiInfoRequest)), WAIT_TIMEOUT);

        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);

        assertThat(jsonResp.get("action").getAsString(), is("server/info"));
        assertThat(jsonResp.get("requestId").getAsString(), is(requestId));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
    }

}

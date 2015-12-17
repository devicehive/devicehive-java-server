package com.devicehive.websockets;

import com.devicehive.base.AbstractWebSocketTest;
import com.devicehive.base.fixture.JsonFixture;
import com.devicehive.base.fixture.WebSocketFixture;
import com.devicehive.base.websocket.WebSocketSynchronousConnection;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.After;
import org.junit.Test;
import org.springframework.web.socket.TextMessage;

import java.util.HashMap;
import java.util.Optional;

import static com.devicehive.base.websocket.WebSocketSynchronousConnection.WAIT_TIMEOUT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CommandHandlersTest extends AbstractWebSocketTest {

    @After
    public void tearDown() throws Exception {
        clearWSConnections();
    }

    @Test
    public void should_insert_command_signed_in_as_admin() throws Exception {
        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");
        WebSocketFixture.authenticateUser(ADMIN_LOGIN, ADMIN_PASS, connection);

        DeviceCommandWrapper command = new DeviceCommandWrapper();
        command.setCommand(Optional.of("test command"));

        JsonObject commandInsert = JsonFixture.createWsCommand("command/insert", "1", new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(DEVICE_ID));
            put("command", gson.toJsonTree(command));
        }});
        long time = System.currentTimeMillis();
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(commandInsert)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("command/insert"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
        assertThat(jsonResp.get("command"), notNullValue());
        InsertCommand commandResp = gson.fromJson(jsonResp.get("command"),InsertCommand.class);
        assertThat(commandResp.getId(), notNullValue());
        assertThat(commandResp.getTimestamp(), notNullValue());
        assertTrue(commandResp.getTimestamp().getTime() > time);
    }

    @Test
    public void should_insert_command_signed_in_as_key() throws Exception {
        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");
        WebSocketFixture.authenticateKey(ACCESS_KEY, connection);

        DeviceCommandWrapper command = new DeviceCommandWrapper();
        command.setCommand(Optional.of("test command"));

        JsonObject commandInsert = JsonFixture.createWsCommand("command/insert", "1", new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(DEVICE_ID));
            put("command", gson.toJsonTree(command));
        }});
        long time = System.currentTimeMillis();
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(commandInsert)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("command/insert"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
        assertThat(jsonResp.get("command"), notNullValue());
        InsertCommand commandResp = gson.fromJson(jsonResp.get("command"),InsertCommand.class);
        assertThat(commandResp.getId(), notNullValue());
        assertThat(commandResp.getTimestamp(), notNullValue());
        assertTrue(commandResp.getTimestamp().getTime() > time);
    }
}

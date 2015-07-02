package com.devicehive.websockets;

import com.devicehive.base.AbstractWebSocketTest;
import com.devicehive.base.fixture.JsonFixture;
import com.devicehive.base.websocket.WebSocketSynchronousConnection;
import com.devicehive.model.*;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.springframework.web.socket.TextMessage;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static com.devicehive.base.websocket.WebSocketSynchronousConnection.WAIT_TIMEOUT;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class NotificationHandlersTest extends AbstractWebSocketTest {

    @Test
    public void should_insert_notification_signed_in_as_device() throws Exception {
        WebSocketSynchronousConnection connection = syncConnection("/websocket/device");
        Pair<String, String> idAndKey = Pair.of("E50D6085-2ABA-48E9-B1C3-73C673E414BE", "05F94BF509C8");

        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("i'm alive");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", "1", idAndKey.getKey(), idAndKey.getValue(),
                singletonMap("notification", gson.toJsonTree(notification)));
        long time = System.currentTimeMillis();
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(notificationInsert)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/insert"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
        assertThat(jsonResp.get("notification"), notNullValue());
        DeviceNotification notificationResp = gson.fromJson(jsonResp.get("notification"), DeviceNotification.class);
        assertThat(notificationResp.getId(), notNullValue());
        assertThat(notificationResp.getDeviceGuid(), is(idAndKey.getKey()));
        assertThat(notificationResp.getTimestamp(), notNullValue());
        assertTrue(notificationResp.getTimestamp().getTime() > time);
        assertThat(notificationResp.getNotification(), is(notification.getNotification()));
        connection.stop();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void should_insert_notification_signed_in_as_admin() throws Exception {
        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");

        JsonObject authRequest = JsonFixture.userAuthCommand("1", "test_admin", "admin_pass");
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(authRequest)), WAIT_TIMEOUT);
        JsonObject authResponse = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(authResponse.get("action").getAsString(), is("authenticate"));
        assertThat(authResponse.get("requestId").getAsString(), is("1"));
        assertThat(authResponse.get("status").getAsString(), is("success"));

        String deviceGuid = "E50D6085-2ABA-48E9-B1C3-73C673E414BE";

        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("hi there");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", "1", new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(deviceGuid));
            put("notification", gson.toJsonTree(notification));
        }});
        long time = System.currentTimeMillis();
        response = connection.sendMessage(new TextMessage(gson.toJson(notificationInsert)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/insert"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
        assertThat(jsonResp.get("notification"), notNullValue());
        DeviceNotification notificationResp = gson.fromJson(jsonResp.get("notification"), DeviceNotification.class);
        assertThat(notificationResp.getId(), notNullValue());
        assertThat(notificationResp.getDeviceGuid(), is(deviceGuid));
        assertThat(notificationResp.getTimestamp(), notNullValue());
        assertTrue(notificationResp.getTimestamp().getTime() > time);
        assertThat(notificationResp.getNotification(), is(notification.getNotification()));
        connection.stop();
    }

    @Test
    public void should_insert_notification_signed_in_as_key() throws Exception {
        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");

        JsonObject authRequest = JsonFixture.keyAuthCommand("1", "1jwKgLYi/CdfBTI9KByfYxwyQ6HUIEfnGSgakdpFjgk=");
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(authRequest)), WAIT_TIMEOUT);
        JsonObject authResponse = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(authResponse.get("action").getAsString(), is("authenticate"));
        assertThat(authResponse.get("requestId").getAsString(), is("1"));
        assertThat(authResponse.get("status").getAsString(), is("success"));

        String deviceGuid = "E50D6085-2ABA-48E9-B1C3-73C673E414BE";

        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("hi there");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", "1", new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(deviceGuid));
            put("notification", gson.toJsonTree(notification));
        }});
        long time = System.currentTimeMillis();
        response = connection.sendMessage(new TextMessage(gson.toJson(notificationInsert)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/insert"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
        assertThat(jsonResp.get("notification"), notNullValue());
        DeviceNotification notificationResp = gson.fromJson(jsonResp.get("notification"), DeviceNotification.class);
        assertThat(notificationResp.getId(), notNullValue());
        assertThat(notificationResp.getDeviceGuid(), is(deviceGuid));
        assertThat(notificationResp.getTimestamp(), notNullValue());
        assertTrue(notificationResp.getTimestamp().getTime() > time);
        assertThat(notificationResp.getNotification(), is(notification.getNotification()));
        connection.stop();
    }

    @Test
    public void should_return_401_response_for_anonymous() throws Exception {
        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");

        String deviceGuid = "E50D6085-2ABA-48E9-B1C3-73C673E414BE";

        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("hi there");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", "1", new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(deviceGuid));
            put("notification", gson.toJsonTree(notification));
        }});
        long time = System.currentTimeMillis();
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(notificationInsert)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/insert"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("error"));
        assertThat(jsonResp.get("code").getAsInt(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        assertThat(jsonResp.get("error").getAsString(), is(Response.Status.UNAUTHORIZED.getReasonPhrase()));

        connection.stop();
    }

    //fixme: authorized requests without permissions should return 403 error instead of 401
    @Test
    public void should_return_401_response_for_key_if_action_is_not_allowed() throws Exception {
        AccessKey accessKey = new AccessKey();
        accessKey.setLabel(UUID.randomUUID().toString());
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setActionsArray(AvailableActions.GET_DEVICE);
        accessKey.setPermissions(Collections.singleton(permission));
        AccessKey createKey = performRequest("/user/1/accesskey", "POST", emptyMap(),
                singletonMap("Authorization", basicAuthHeader("test_admin", "admin_pass")), accessKey, CREATED, AccessKey.class);
        assertThat(createKey, notNullValue());
        assertThat(createKey.getKey(), notNullValue());

        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");
        JsonObject authRequest = JsonFixture.keyAuthCommand("1", createKey.getKey());
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(authRequest)), WAIT_TIMEOUT);
        JsonObject authResponse = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(authResponse.get("action").getAsString(), is("authenticate"));
        assertThat(authResponse.get("requestId").getAsString(), is("1"));
        assertThat(authResponse.get("status").getAsString(), is("success"));

        String deviceGuid = "E50D6085-2ABA-48E9-B1C3-73C673E414BE";

        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("hi there");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", "2", new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(deviceGuid));
            put("notification", gson.toJsonTree(notification));
        }});
        response = connection.sendMessage(new TextMessage(gson.toJson(notificationInsert)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/insert"));
        assertThat(jsonResp.get("requestId").getAsString(), is("2"));
        assertThat(jsonResp.get("code").getAsInt(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        assertThat(jsonResp.get("error").getAsString(), is(Response.Status.UNAUTHORIZED.getReasonPhrase()));
        connection.stop();
    }

    //fixme: authorized requests without permissions should return 403 error instead of 401
    @Test
    public void should_return_401_status_for_device_during_subscribe_action() throws Exception {
        String deviceId = "E50D6085-2ABA-48E9-B1C3-73C673E414BE";
        String deviceKey = "05F94BF509C8";
        JsonObject notificationSubscribe = JsonFixture.createWsCommand("notification/subscribe", "1", deviceId, deviceKey,
                singletonMap("names", gson.toJsonTree(Collections.singleton("some_name"))));
        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(notificationSubscribe)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/subscribe"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("error"));
        assertThat(jsonResp.get("code").getAsInt(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        assertThat(jsonResp.get("error").getAsString(), is(Response.Status.UNAUTHORIZED.getReasonPhrase()));
        connection.stop();
    }

    //fixme: authorized requests without permissions should return 403 error instead of 401
    @Test
    public void should_return_401_status_for_key_without_permission_to_subscribe() throws Exception {
        AccessKey accessKey = new AccessKey();
        accessKey.setLabel(UUID.randomUUID().toString());
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setActionsArray(AvailableActions.GET_DEVICE);
        accessKey.setPermissions(Collections.singleton(permission));
        AccessKey createKey = performRequest("/user/1/accesskey", "POST", emptyMap(),
                singletonMap("Authorization", basicAuthHeader("test_admin", "admin_pass")), accessKey, CREATED, AccessKey.class);
        assertThat(createKey, notNullValue());
        assertThat(createKey.getKey(), notNullValue());

        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");
        JsonObject authRequest = JsonFixture.keyAuthCommand("1", createKey.getKey());
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(authRequest)), WAIT_TIMEOUT);
        JsonObject authResponse = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(authResponse.get("action").getAsString(), is("authenticate"));
        assertThat(authResponse.get("requestId").getAsString(), is("1"));
        assertThat(authResponse.get("status").getAsString(), is("success"));

        JsonObject notificationSubscribe = JsonFixture.createWsCommand("notification/subscribe", "2", singletonMap("names", gson.toJsonTree(Collections.singleton("some_name"))));
        response = connection.sendMessage(new TextMessage(gson.toJson(notificationSubscribe)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/subscribe"));
        assertThat(jsonResp.get("requestId").getAsString(), is("2"));
        assertThat(jsonResp.get("status").getAsString(), is("error"));
        assertThat(jsonResp.get("code").getAsInt(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        assertThat(jsonResp.get("error").getAsString(), is(Response.Status.UNAUTHORIZED.getReasonPhrase()));
        connection.stop();
    }

    @Test
    public void should_proceed_with_subscription_for_admin() throws Exception {
        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");

        JsonObject authRequest = JsonFixture.userAuthCommand("1", "test_admin", "admin_pass");
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(authRequest)), WAIT_TIMEOUT);
        JsonObject authResponse = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(authResponse.get("action").getAsString(), is("authenticate"));
        assertThat(authResponse.get("requestId").getAsString(), is("1"));
        assertThat(authResponse.get("status").getAsString(), is("success"));

        JsonObject notificationSubscribe = JsonFixture.createWsCommand("notification/subscribe", "2", singletonMap("names", gson.toJsonTree(Collections.singleton("some_name"))));
        response = connection.sendMessage(new TextMessage(gson.toJson(notificationSubscribe)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/subscribe"));
        assertThat(jsonResp.get("requestId").getAsString(), is("2"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
        assertThat(jsonResp.get("subscriptionId").getAsString(), notNullValue());
    }

}

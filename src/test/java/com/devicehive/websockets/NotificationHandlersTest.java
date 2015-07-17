package com.devicehive.websockets;

import com.devicehive.base.AbstractWebSocketTest;
import com.devicehive.base.fixture.JsonFixture;
import com.devicehive.base.fixture.WebSocketFixture;
import com.devicehive.base.websocket.WebSocketSynchronousConnection;
import com.devicehive.model.*;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.socket.TextMessage;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

    @After
    public void tearDown() throws Exception {
        clearWSConnections();
    }

    @Test
    public void should_insert_notification_signed_in_as_device() throws Exception {
        WebSocketSynchronousConnection connection = syncConnection("/websocket/device");
        Pair<String, String> idAndKey = Pair.of(DEVICE_ID, DEVICE_KEY);

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
    }

    @SuppressWarnings("unchecked")
    @Test
    public void should_insert_notification_signed_in_as_admin() throws Exception {
        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");
        WebSocketFixture.authenticateUser(ADMIN_LOGIN, "admin_pass", connection);

        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("hi there");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", "1", new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(DEVICE_ID));
            put("notification", gson.toJsonTree(notification));
        }});
        long time = System.currentTimeMillis();
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(notificationInsert)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/insert"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
        assertThat(jsonResp.get("notification"), notNullValue());
        DeviceNotification notificationResp = gson.fromJson(jsonResp.get("notification"), DeviceNotification.class);
        assertThat(notificationResp.getId(), notNullValue());
        assertThat(notificationResp.getDeviceGuid(), is(DEVICE_ID));
        assertThat(notificationResp.getTimestamp(), notNullValue());
        assertTrue(notificationResp.getTimestamp().getTime() > time);
        assertThat(notificationResp.getNotification(), is(notification.getNotification()));
    }

    @Test
    public void should_insert_notification_signed_in_as_key() throws Exception {
        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");
        WebSocketFixture.authenticateKey(ACCESS_KEY, connection);

        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("hi there");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", "1", new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(DEVICE_ID));
            put("notification", gson.toJsonTree(notification));
        }});
        long time = System.currentTimeMillis();
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(notificationInsert)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/insert"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
        assertThat(jsonResp.get("notification"), notNullValue());
        DeviceNotification notificationResp = gson.fromJson(jsonResp.get("notification"), DeviceNotification.class);
        assertThat(notificationResp.getId(), notNullValue());
        assertThat(notificationResp.getDeviceGuid(), is(DEVICE_ID));
        assertThat(notificationResp.getTimestamp(), notNullValue());
        assertTrue(notificationResp.getTimestamp().getTime() > time);
        assertThat(notificationResp.getNotification(), is(notification.getNotification()));
    }

    @Test
    public void should_return_401_response_for_anonymous() throws Exception {
        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");

        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("hi there");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", "1", new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(DEVICE_ID));
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
                singletonMap("Authorization", basicAuthHeader(ADMIN_LOGIN, "admin_pass")), accessKey, CREATED, AccessKey.class);
        assertThat(createKey, notNullValue());
        assertThat(createKey.getKey(), notNullValue());

        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");
        WebSocketFixture.authenticateKey(createKey.getKey(), connection);

        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("hi there");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", "2", new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(DEVICE_ID));
            put("notification", gson.toJsonTree(notification));
        }});
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(notificationInsert)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/insert"));
        assertThat(jsonResp.get("requestId").getAsString(), is("2"));
        assertThat(jsonResp.get("code").getAsInt(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        assertThat(jsonResp.get("error").getAsString(), is(Response.Status.UNAUTHORIZED.getReasonPhrase()));
    }

    //fixme: authorized requests without permissions should return 403 error instead of 401
    @Test
    public void should_return_401_status_for_device_during_subscribe_action() throws Exception {
        JsonObject notificationSubscribe = JsonFixture.createWsCommand("notification/subscribe", "1", DEVICE_ID, DEVICE_KEY,
                singletonMap("names", gson.toJsonTree(Collections.singleton("some_name"))));
        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(notificationSubscribe)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/subscribe"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("error"));
        assertThat(jsonResp.get("code").getAsInt(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        assertThat(jsonResp.get("error").getAsString(), is(Response.Status.UNAUTHORIZED.getReasonPhrase()));
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
                singletonMap("Authorization", basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), accessKey, CREATED, AccessKey.class);
        assertThat(createKey, notNullValue());
        assertThat(createKey.getKey(), notNullValue());

        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");
        WebSocketFixture.authenticateKey(createKey.getKey(), connection);

        JsonObject notificationSubscribe = JsonFixture.createWsCommand("notification/subscribe", "2", singletonMap("names", gson.toJsonTree(Collections.singleton("some_name"))));
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(notificationSubscribe)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/subscribe"));
        assertThat(jsonResp.get("requestId").getAsString(), is("2"));
        assertThat(jsonResp.get("status").getAsString(), is("error"));
        assertThat(jsonResp.get("code").getAsInt(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        assertThat(jsonResp.get("error").getAsString(), is(Response.Status.UNAUTHORIZED.getReasonPhrase()));
    }

    @Test
    public void should_proceed_with_subscription_for_admin() throws Exception {
        WebSocketSynchronousConnection connection = syncConnection("/websocket/client");
        WebSocketFixture.authenticateUser(ADMIN_LOGIN, ADMIN_PASS, connection);

        JsonObject notificationSubscribe = JsonFixture.createWsCommand("notification/subscribe", "2", singletonMap("names", gson.toJsonTree(Collections.singleton("some_name"))));
        TextMessage response = connection.sendMessage(new TextMessage(gson.toJson(notificationSubscribe)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/subscribe"));
        assertThat(jsonResp.get("requestId").getAsString(), is("2"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
        assertThat(jsonResp.get("subscriptionId").getAsString(), notNullValue());
    }

    @Test
    public void should_receive_notification_from_device_after_subscription() throws Exception{
        WebSocketSynchronousConnection client = syncConnection("/websocket/client");
        WebSocketFixture.authenticateUser(ADMIN_LOGIN, ADMIN_PASS, client);

        String request = RandomStringUtils.random(5);
        JsonObject notificationSubscribe = JsonFixture.createWsCommand("notification/subscribe", request, singletonMap("deviceGuid", new JsonPrimitive(DEVICE_ID)));
        TextMessage response = client.sendMessage(new TextMessage(gson.toJson(notificationSubscribe)), WAIT_TIMEOUT);
        JsonObject jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("requestId").getAsString(), is(request));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
        assertThat(jsonResp.get("subscriptionId"), notNullValue());
        String subscriptionId = jsonResp.get("subscriptionId").getAsString();
        assertThat(subscriptionId, notNullValue());

        WebSocketSynchronousConnection device = syncConnection("/websocket/device");
        WebSocketFixture.authenticateDevice(DEVICE_ID, DEVICE_KEY, device);

        request = RandomStringUtils.random(5);
        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("hi there");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", request, new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(DEVICE_ID));
            put("notification", gson.toJsonTree(notification));
        }});
        response = device.sendMessage(new TextMessage(gson.toJson(notificationInsert)), WAIT_TIMEOUT);
        jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("requestId").getAsString(), is(request));
        assertThat(jsonResp.get("status").getAsString(), is("success"));

        response = client.pollMessage(WAIT_TIMEOUT);
        assertThat(response, notNullValue());
        jsonResp = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/insert"));
        assertThat(jsonResp.get("deviceGuid").getAsString(), is(DEVICE_ID));
        assertThat(jsonResp.get("subscriptionId").getAsString(), is(subscriptionId));
        assertThat(jsonResp.get("notification"), notNullValue());
        DeviceNotification notificationResponse = gson.fromJson(jsonResp.get("notification"), DeviceNotification.class);
        assertThat(notificationResponse.getNotification(), is("hi there"));
        assertThat(notificationResponse.getDeviceGuid(), is(DEVICE_ID));
        assertThat(notificationResponse.getTimestamp(), notNullValue());
        assertThat(notificationResponse.getId(), notNullValue());
    }

}

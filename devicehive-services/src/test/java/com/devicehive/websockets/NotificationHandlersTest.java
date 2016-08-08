package com.devicehive.websockets;

import com.devicehive.base.AbstractWebSocketMethodTest;
import com.devicehive.base.fixture.JsonFixture;
import com.devicehive.model.*;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.vo.AccessKeyPermissionVO;
import com.devicehive.vo.AccessKeyVO;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class NotificationHandlersTest extends AbstractWebSocketMethodTest {

    @Test
    public void should_insert_notification_signed_in_as_admin() throws Exception {
        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("hi there");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", "1", new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(DEVICE_ID));
            put("notification", gson.toJsonTree(notification));
        }});
        long time = System.currentTimeMillis();

        String payload = runMethod(notificationInsert, auth(ADMIN_LOGIN, ADMIN_PASS));
        JsonObject jsonResp = gson.fromJson(payload, JsonObject.class);

        assertThat(jsonResp.get("action").getAsString(), is("notification/insert"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
        assertThat(jsonResp.get("notification"), notNullValue());

        InsertNotification notificationResp = gson.fromJson(jsonResp.get("notification"), InsertNotification.class);
        assertThat(notificationResp.getId(), notNullValue());
        assertThat(notificationResp.getTimestamp(), notNullValue());
        assertTrue(notificationResp.getTimestamp().getTime() > time);
    }

    @Test
    public void should_insert_notification_signed_in_as_key() throws Exception {
        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("hi there");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", "1", new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(DEVICE_ID));
            put("notification", gson.toJsonTree(notification));
        }});
        long time = System.currentTimeMillis();
        String payload = runMethod(notificationInsert, auth(ACCESS_KEY));
        JsonObject jsonResp = gson.fromJson(payload, JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/insert"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
        assertThat(jsonResp.get("notification"), notNullValue());
        InsertNotification notificationResp = gson.fromJson(jsonResp.get("notification"),InsertNotification.class);
        assertThat(notificationResp.getId(), notNullValue());
        assertThat(notificationResp.getTimestamp(), notNullValue());
        assertTrue(notificationResp.getTimestamp().getTime() > time);
    }

    @Test
    public void should_return_401_response_for_anonymous() throws Exception {
        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("hi there");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", "1", new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(DEVICE_ID));
            put("notification", gson.toJsonTree(notification));
        }});
        String payload = runMethod(notificationInsert, auth());
        JsonObject jsonResp = gson.fromJson(payload, JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/insert"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("error"));
        assertThat(jsonResp.get("code").getAsInt(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        assertThat(jsonResp.get("error").getAsString(), is(Response.Status.UNAUTHORIZED.getReasonPhrase()));
    }

    //fixme: authorized requests without permissions should return 403 error instead of 401
    @Test
    public void should_return_401_response_for_key_if_action_is_not_allowed() throws Exception {
        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setLabel(UUID.randomUUID().toString());
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setActionsArray(AvailableActions.GET_DEVICE);
        accessKey.setPermissions(Collections.singleton(permission));
        AccessKeyVO createKey = performRequest("/user/1/accesskey", "POST", emptyMap(),
                singletonMap("Authorization", basicAuthHeader(ADMIN_LOGIN, "admin_pass")), accessKey, CREATED, AccessKeyVO.class);
        assertThat(createKey, notNullValue());
        assertThat(createKey.getKey(), notNullValue());

        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("hi there");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", "2", new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(DEVICE_ID));
            put("notification", gson.toJsonTree(notification));
        }});
        String result = runMethod(notificationInsert, auth(createKey.getKey()));
        JsonObject jsonResp = gson.fromJson(result, JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/insert"));
        assertThat(jsonResp.get("requestId").getAsString(), is("2"));
        assertThat(jsonResp.get("code").getAsInt(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        assertThat(jsonResp.get("error").getAsString(), is(Response.Status.UNAUTHORIZED.getReasonPhrase()));
    }

    //fixme: authorized requests without permissions should return 403 error instead of 401
    @Test
    public void should_return_401_status_for_device_during_subscribe_action() throws Exception {
        JsonObject notificationSubscribe = JsonFixture.createWsCommand("notification/subscribe", "1", DEVICE_ID,
                singletonMap("names", gson.toJsonTree(Collections.singleton("some_name"))));

        String result = runMethod(notificationSubscribe, auth());
        JsonObject jsonResp = gson.fromJson(result, JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/subscribe"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("error"));
        assertThat(jsonResp.get("code").getAsInt(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        assertThat(jsonResp.get("error").getAsString(), is(Response.Status.UNAUTHORIZED.getReasonPhrase()));
    }

    //fixme: authorized requests without permissions should return 403 error instead of 401
    @Test
    public void should_return_401_status_for_key_without_permission_to_subscribe() throws Exception {
        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setLabel(UUID.randomUUID().toString());
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setActionsArray(AvailableActions.GET_DEVICE);
        accessKey.setPermissions(Collections.singleton(permission));
        AccessKeyVO createKey = performRequest("/user/1/accesskey", "POST", emptyMap(),
                singletonMap("Authorization", basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), accessKey, CREATED, AccessKeyVO.class);
        assertThat(createKey, notNullValue());
        assertThat(createKey.getKey(), notNullValue());

        JsonObject notificationSubscribe = JsonFixture.createWsCommand("notification/subscribe", "2", singletonMap("names", gson.toJsonTree(Collections.singleton("some_name"))));
        String result = runMethod(notificationSubscribe, auth(createKey.getKey()));
        JsonObject jsonResp = gson.fromJson(result, JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/subscribe"));
        assertThat(jsonResp.get("requestId").getAsString(), is("2"));
        assertThat(jsonResp.get("status").getAsString(), is("error"));
        assertThat(jsonResp.get("code").getAsInt(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        assertThat(jsonResp.get("error").getAsString(), is(Response.Status.UNAUTHORIZED.getReasonPhrase()));
    }

    @Test
    public void should_proceed_with_subscription_for_admin() throws Exception {
        JsonObject notificationSubscribe = JsonFixture.createWsCommand("notification/subscribe", "2", singletonMap("names", gson.toJsonTree(Collections.singleton("some_name"))));
        String result = runMethod(notificationSubscribe, auth(ADMIN_LOGIN, ADMIN_PASS));
        JsonObject jsonResp = gson.fromJson(result, JsonObject.class);
        assertThat(jsonResp.get("action").getAsString(), is("notification/subscribe"));
        assertThat(jsonResp.get("requestId").getAsString(), is("2"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
        assertThat(jsonResp.get("subscriptionId").getAsString(), notNullValue());
    }

    @Test
    public void should_receive_notification_from_device_after_subscription() throws Exception{
        String request = RandomStringUtils.random(5);
        JsonObject notificationSubscribe = JsonFixture.createWsCommand("notification/subscribe", request, singletonMap("deviceGuid", new JsonPrimitive(DEVICE_ID)));
        String result = runMethod(notificationSubscribe, auth(ADMIN_LOGIN, ADMIN_PASS));
        JsonObject jsonResp = gson.fromJson(result, JsonObject.class);
        assertThat(jsonResp.get("requestId").getAsString(), is(request));
        assertThat(jsonResp.get("status").getAsString(), is("success"));
        assertThat(jsonResp.get("subscriptionId"), notNullValue());
        String subscriptionId = jsonResp.get("subscriptionId").getAsString();
        assertThat(subscriptionId, notNullValue());

        request = RandomStringUtils.random(5);
        DeviceNotificationWrapper notification = new DeviceNotificationWrapper();
        notification.setNotification("hi there");
        notification.setParameters(new JsonStringWrapper("{\"param\": \"param_1\"}"));
        JsonObject notificationInsert = JsonFixture.createWsCommand("notification/insert", request, new HashMap<String, JsonElement>() {{
            put("deviceGuid", new JsonPrimitive(DEVICE_ID));
            put("notification", gson.toJsonTree(notification));
        }});

        HiveWebsocketSessionState storedState = this.state;

        result = runMethod(notificationInsert, auth(ACCESS_KEY));
        jsonResp = gson.fromJson(result, JsonObject.class);
        assertThat(jsonResp.get("requestId").getAsString(), is(request));
        assertThat(jsonResp.get("status").getAsString(), is("success"));

        while (true) {
            try {
                storedState.getQueueLock().lock();
                if (!storedState.getQueue().isEmpty()) {
                    break;
                }
                Thread.sleep(100);
            } finally {
                storedState.getQueueLock().unlock();
            }
        }

        assertThat(storedState.getQueue().peek(), notNullValue());
        jsonResp = gson.fromJson(storedState.getQueue().poll(), JsonObject.class);
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

package com.devicehive.handler.notification;

import com.devicehive.base.AbstractSpringTest;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.eventbus.events.NotificationEvent;
import com.devicehive.model.rpc.Action;
import com.devicehive.model.rpc.NotificationInsertRequest;
import com.devicehive.model.rpc.NotificationSubscribeRequest;
import com.devicehive.model.rpc.NotificationSubscribeResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class NotificationSubscribeInsertIntegrationTest extends AbstractSpringTest {

    @Autowired
    private RpcClient client;

    @Test
    public void shouldSubscribeToDeviceNotifications() throws Exception {
        String device1 = randomUUID().toString();
        String device2 = randomUUID().toString();

        String subscriber1 = randomUUID().toString();
        String subscriber2 = randomUUID().toString();
        String subscriber3 = randomUUID().toString();

        NotificationSubscribeRequest sr1 = new NotificationSubscribeRequest(subscriber1, device1, null, null);
        Request r1 = Request.newBuilder().withBody(sr1).withSingleReply(false).build();
        TestCallback c1 = new TestCallback();
        client.call(r1, c1);

        NotificationSubscribeRequest sr2 = new NotificationSubscribeRequest(subscriber1, device2,
                Collections.singleton("temperature"), null);
        Request r2 = Request.newBuilder().withBody(sr2).withSingleReply(false).build();
        TestCallback c2 = new TestCallback();
        client.call(r2, c2);

        NotificationSubscribeRequest sr3 = new NotificationSubscribeRequest(subscriber2, device2, null, null);
        Request r3 = Request.newBuilder().withBody(sr3).withSingleReply(false).build();
        TestCallback c3 = new TestCallback();
        client.call(r3, c3);

        NotificationSubscribeRequest sr4 = new NotificationSubscribeRequest(subscriber2, device1,
                Collections.singleton("vibration"), null);
        Request r4 = Request.newBuilder().withBody(sr4).withSingleReply(false).build();
        TestCallback c4 = new TestCallback();
        client.call(r4, c4);

        NotificationSubscribeRequest sr5 = new NotificationSubscribeRequest(subscriber3, randomUUID().toString(), null, null);
        Request r5 = Request.newBuilder().withBody(sr5).withSingleReply(false).build();
        TestCallback c5 = new TestCallback();
        client.call(r5, c5);

        //wait subsribers to subscribe
        Stream.of(c1.subscribeFuture, c2.subscribeFuture, c3.subscribeFuture, c4.subscribeFuture, c5.subscribeFuture)
                .forEach(CompletableFuture::join);

        //devices send notifications
        List<CompletableFuture<Response>> futures = Stream.of(device1, device2).flatMap(device -> {
            List<CompletableFuture<Response>> list = Stream.of("temperature", "vibration").map(name -> {
                DeviceNotification notification = new DeviceNotification();
                notification.setId(0);
                notification.setNotification(name);
                notification.setDeviceGuid(device);
                NotificationInsertRequest event = new NotificationInsertRequest(notification);
                CompletableFuture<Response> f = new CompletableFuture<>();
                client.call(Request.newBuilder().withBody(event).build(), f::complete);
                return f;
            }).collect(Collectors.toList());
            return list.stream();
        }).collect(Collectors.toList());

        //wait notifications delivered
        futures.forEach(CompletableFuture::join);

        assertThat(c1.notifications, hasSize(2));
        c1.notifications.forEach(event -> {
            assertNotNull(event.getNotification());
            assertEquals(event.getNotification().getDeviceGuid(), device1);
            assertEquals(event.getNotification().getId(), Long.valueOf(0));
        });
        Set<String> names = c1.notifications.stream()
                .map(n -> n.getNotification().getNotification())
                .collect(Collectors.toSet());
        assertThat(names, containsInAnyOrder("temperature", "vibration"));

        assertThat(c2.notifications, hasSize(1));
        NotificationEvent e = c2.notifications.stream().findFirst().get();
        assertNotNull(e.getNotification());
        assertEquals(e.getNotification().getDeviceGuid(), device2);
        assertEquals(e.getNotification().getId(), Long.valueOf(0));
        assertEquals(e.getNotification().getNotification(), "temperature");

        assertThat(c3.notifications, hasSize(2));
        c3.notifications.forEach(event -> {
            assertNotNull(event.getNotification());
            assertEquals(event.getNotification().getDeviceGuid(), device2);
            assertEquals(event.getNotification().getId(), Long.valueOf(0));
        });
        names = c3.notifications.stream()
                .map(n -> n.getNotification().getNotification())
                .collect(Collectors.toSet());
        assertThat(names, containsInAnyOrder("temperature", "vibration"));

        assertThat(c4.notifications, hasSize(1));
        e = c4.notifications.stream().findFirst().get();
        assertNotNull(e.getNotification());
        assertEquals(e.getNotification().getDeviceGuid(), device1);
        assertEquals(e.getNotification().getId(), Long.valueOf(0));
        assertEquals(e.getNotification().getNotification(), "vibration");

        assertThat(c5.notifications, is(empty()));
    }

    public static class TestCallback implements Consumer<Response> {

        private CompletableFuture<NotificationSubscribeResponse> subscribeFuture;
        private Set<NotificationEvent> notifications;

        public TestCallback() {
            this.subscribeFuture = new CompletableFuture<>();
            this.notifications = new HashSet<>();
        }

        @Override
        public void accept(Response response) {
            if (response.getBody().getAction().equals(Action.NOTIFICATION_SUBSCRIBE_RESPONSE.name())) {
                subscribeFuture.complete((NotificationSubscribeResponse) response.getBody());
            } else if (response.getBody().getAction().equals(Action.NOTIFICATION.name())) {
                notifications.add((NotificationEvent) response.getBody());
            } else {
                throw new IllegalArgumentException("Unexpected response " + response);
            }
        }
    }

}

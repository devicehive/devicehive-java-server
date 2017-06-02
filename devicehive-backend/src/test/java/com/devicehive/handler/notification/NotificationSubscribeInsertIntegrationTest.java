package com.devicehive.handler.notification;

/*
 * #%L
 * DeviceHive Backend Logic
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

import com.devicehive.base.AbstractSpringTest;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.eventbus.events.NotificationEvent;
import com.devicehive.model.rpc.*;
import com.devicehive.shim.api.Body;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
        List<CompletableFuture<Response>> futures = Stream.of(device1, device2).flatMap(deviceId -> {
            List<CompletableFuture<Response>> list = Stream.of("temperature", "vibration").map(name -> {
                DeviceNotification notification = new DeviceNotification();
                notification.setId(0);
                notification.setNotification(name);
                notification.setDeviceId(deviceId);
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
            assertEquals(event.getNotification().getDeviceId(), device1);
            assertEquals(event.getNotification().getId(), Long.valueOf(0));
        });
        Set<String> names = c1.notifications.stream()
                .map(n -> n.getNotification().getNotification())
                .collect(Collectors.toSet());
        assertThat(names, containsInAnyOrder("temperature", "vibration"));

        assertThat(c2.notifications, hasSize(1));
        NotificationEvent e = c2.notifications.stream().findFirst().get();
        assertNotNull(e.getNotification());
        assertEquals(e.getNotification().getDeviceId(), device2);
        assertEquals(e.getNotification().getId(), Long.valueOf(0));
        assertEquals(e.getNotification().getNotification(), "temperature");

        assertThat(c3.notifications, hasSize(2));
        c3.notifications.forEach(event -> {
            assertNotNull(event.getNotification());
            assertEquals(event.getNotification().getDeviceId(), device2);
            assertEquals(event.getNotification().getId(), Long.valueOf(0));
        });
        names = c3.notifications.stream()
                .map(n -> n.getNotification().getNotification())
                .collect(Collectors.toSet());
        assertThat(names, containsInAnyOrder("temperature", "vibration"));

        assertThat(c4.notifications, hasSize(1));
        e = c4.notifications.stream().findFirst().get();
        assertNotNull(e.getNotification());
        assertEquals(e.getNotification().getDeviceId(), device1);
        assertEquals(e.getNotification().getId(), Long.valueOf(0));
        assertEquals(e.getNotification().getNotification(), "vibration");

        assertThat(c5.notifications, is(empty()));
    }

    @Test
    @Ignore
    public void shouldUnsubscribeFromNotifications() throws Exception {
        String device1 = randomUUID().toString();

        String subscriber1 = randomUUID().toString();
        String subscriber2 = randomUUID().toString();

        NotificationSubscribeRequest sr1 = new NotificationSubscribeRequest(subscriber1, device1, null, null);
        Request r1 = Request.newBuilder().withBody(sr1).withSingleReply(false).build();
        TestCallback c1 = new TestCallback();
        client.call(r1, c1);

        NotificationSubscribeRequest sr2 = new NotificationSubscribeRequest(subscriber2, device1, null, null);
        Request r2 = Request.newBuilder().withBody(sr2).withSingleReply(false).build();
        TestCallback c2 = new TestCallback();
        client.call(r2, c2);

        Stream.of(c1.subscribeFuture, c2.subscribeFuture).forEach(CompletableFuture::join);

        DeviceNotification notification = new DeviceNotification();
        notification.setId(0);
        notification.setNotification("temperature");
        notification.setDeviceId(device1);
        NotificationInsertRequest event = new NotificationInsertRequest(notification);
        CompletableFuture<Response> f1 = new CompletableFuture<>();
        client.call(Request.newBuilder().withBody(event).build(), f1::complete);

        f1.get(15, TimeUnit.SECONDS);

        assertThat(c1.notifications, hasSize(1));
        assertThat(c2.notifications, hasSize(1));

        NotificationUnsubscribeRequest ur = new NotificationUnsubscribeRequest(sr1.getSubscriptionId(), null);
        Request r3 = Request.newBuilder().withBody(ur).withSingleReply(false).build();
        client.call(r3, c1);

        c1.subscribeFuture.join();

        DeviceNotification notification2 = new DeviceNotification();
        notification2.setId(1);
        notification2.setNotification("temperature");
        notification2.setDeviceId(device1);
        NotificationInsertRequest event2 = new NotificationInsertRequest(notification2);
        CompletableFuture<Response> f2 = new CompletableFuture<>();
        client.call(Request.newBuilder().withBody(event2).build(), f2::complete);

        f2.join();

        assertThat(c1.notifications, hasSize(1));
        assertThat(c2.notifications, hasSize(2));
    }

    public static class TestCallback implements Consumer<Response> {

        private CompletableFuture<Body> subscribeFuture;
        private Set<NotificationEvent> notifications;

        public TestCallback() {
            this.subscribeFuture = new CompletableFuture<>();
            this.notifications = new HashSet<>();
        }

        @Override
        public void accept(Response response) {
            if (response.getBody().getAction().equals(Action.NOTIFICATION_SUBSCRIBE_RESPONSE.name())
                    || response.getBody().getAction().equals(Action.NOTIFICATION_UNSUBSCRIBE_RESPONSE.name())) {
                subscribeFuture.complete(response.getBody());
            } else if (response.getBody().getAction().equals(Action.NOTIFICATION_EVENT.name())) {
                notifications.add((NotificationEvent) response.getBody());
            } else {
                throw new IllegalArgumentException("Unexpected response " + response);
            }
        }
    }

}

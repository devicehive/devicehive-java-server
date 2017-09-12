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

import com.devicehive.eventbus.EventBus;
import com.devicehive.eventbus.FilterRegistry;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.shim.api.Action;
import com.devicehive.model.rpc.NotificationSubscribeRequest;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NotificationSubscribeRequestHandlerUnitTest {

    private EventBus eventBus;
    private HazelcastService hazelcastService;
    private NotificationSubscribeRequestHandler handler;

    @Rule
    public ExpectedException ex = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        eventBus = Mockito.mock(EventBus.class);
        hazelcastService = Mockito.mock(HazelcastService.class);
        this.handler = new NotificationSubscribeRequestHandler();
        this.handler.setEventBus(eventBus);
        this.handler.setHazelcastService(hazelcastService);

        this.handler.setFilterRegistry(new FilterRegistry());
    }

    @Test
    public void shouldSubscribeToDeviceNotifications() throws Exception {
        Long subscriptionId = randomUUID().getMostSignificantBits();
        String device = randomUUID().toString();
        NotificationSubscribeRequest sr =
                new NotificationSubscribeRequest(subscriptionId, device, new Filter(), null);
        Request request = Request.newBuilder()
                .withBody(sr)
                .withPartitionKey(randomUUID().toString())
                .withSingleReply(false)
                .build();
        handler.handle(request);

        ArgumentCaptor<Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Subscriber.class);
        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(eventBus).subscribe(subscriberCaptor.capture(), subscriptionCaptor.capture());

        Subscriber subscriber = subscriberCaptor.getValue();
        assertEquals(subscriber.getId(), subscriptionId);
        assertEquals(subscriber.getCorrelationId(), request.getCorrelationId());
        assertEquals(subscriber.getReplyTo(), request.getReplyTo());

        Subscription subscription = subscriptionCaptor.getValue();
        assertEquals(subscription.getType(), Action.NOTIFICATION_EVENT.name());
        assertEquals(subscription.getEntityId(), device);
        assertNull(subscription.getName());

        verifyZeroInteractions(hazelcastService);
    }

    @Test
    public void shouldSubscribeToDeviceNotificationsNames() throws Exception {
        Long subscriptionId = randomUUID().getMostSignificantBits();
        String device = randomUUID().toString();
        Set<String> names = Stream.of("a", "b", "c").collect(Collectors.toSet());
        Filter filter = new Filter();
        filter.setNames(names);
        NotificationSubscribeRequest sr =
                new NotificationSubscribeRequest(subscriptionId, device, filter, null);
        Request request = Request.newBuilder()
                .withBody(sr)
                .withPartitionKey(randomUUID().toString())
                .withSingleReply(false)
                .build();
        handler.handle(request);

        ArgumentCaptor<Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Subscriber.class);
        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(eventBus, times(names.size())).subscribe(subscriberCaptor.capture(), subscriptionCaptor.capture());

        Set<Subscriber> subscribers = subscriberCaptor.getAllValues().stream().collect(Collectors.toSet());
        assertThat(subscribers, hasSize(1));
        Subscriber subscriber = subscribers.stream().findFirst().get();
        assertEquals(subscriber.getReplyTo(), request.getReplyTo());
        assertEquals(subscriber.getId(), subscriptionId);
        assertEquals(subscriber.getCorrelationId(), request.getCorrelationId());

        List<Subscription> subscriptions = subscriptionCaptor.getAllValues();
        assertThat(subscriptions, hasSize(names.size()));
        subscriptions.forEach(subscription -> {
            assertEquals(subscription.getEntityId(), device);
            assertEquals(subscription.getType(), Action.NOTIFICATION_EVENT.name());
        });
        Set<String> notificationNames = subscriptions.stream().map(Subscription::getName).collect(Collectors.toSet());
        assertThat(notificationNames, hasSize(3));
        assertEquals(notificationNames, names);

        verifyZeroInteractions(hazelcastService);
    }

    @Test
    public void shouldSubscribeAndSearchNotificationsInCache() throws Exception {
        Long subscriptionId = randomUUID().getMostSignificantBits();
        String device = randomUUID().toString();
        Set<String> names = Stream.of("a", "b", "c").collect(Collectors.toSet());
        Date timestamp = new Date();
        Filter filter = new Filter();
        filter.setNames(names);
        NotificationSubscribeRequest sr =
                new NotificationSubscribeRequest(subscriptionId, device, filter, timestamp);
        Request request = Request.newBuilder()
                .withBody(sr)
                .withPartitionKey(randomUUID().toString())
                .withSingleReply(false)
                .build();
        handler.handle(request);

        ArgumentCaptor<Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Subscriber.class);
        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(eventBus, times(names.size())).subscribe(subscriberCaptor.capture(), subscriptionCaptor.capture());

        Set<Subscriber> subscribers = subscriberCaptor.getAllValues().stream().collect(Collectors.toSet());
        assertThat(subscribers, hasSize(1));
        Subscriber subscriber = subscribers.stream().findFirst().get();
        assertEquals(subscriber.getReplyTo(), request.getReplyTo());
        assertEquals(subscriber.getId(), subscriptionId);
        assertEquals(subscriber.getCorrelationId(), request.getCorrelationId());

        List<Subscription> subscriptions = subscriptionCaptor.getAllValues();
        assertThat(subscriptions, hasSize(names.size()));
        subscriptions.forEach(subscription -> {
            assertEquals(subscription.getEntityId(), device);
            assertEquals(subscription.getType(), Action.NOTIFICATION_EVENT.name());
        });
        Set<String> notificationNames = subscriptions.stream().map(Subscription::getName).collect(Collectors.toSet());
        assertThat(notificationNames, hasSize(3));
        assertEquals(notificationNames, names);

        verify(hazelcastService).find(null, names, Collections.singleton(device),
                NotificationSubscribeRequestHandler.LIMIT, timestamp, null, false, null, DeviceNotification.class);
    }

    @Test
    public void shouldThrowIfBodyIsNull() throws Exception {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("Request body is null");

        Request request = Request.newBuilder()
                .withPartitionKey(randomUUID().toString())
                .withSingleReply(false)
                .build();
        handler.handle(request);
    }

    @Test
    public void shouldThrowIfSubscriptionIdIsNull() throws Exception {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("Subscription id not provided");

        NotificationSubscribeRequest sr =
                new NotificationSubscribeRequest(null, randomUUID().toString(), null, null);
        Request request = Request.newBuilder()
                .withBody(sr)
                .withPartitionKey(randomUUID().toString())
                .withSingleReply(false)
                .build();
        handler.handle(request);
    }

    @Test
    public void shouldThrowIfDeviceIdIsNull() throws Exception {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("Device id is null");

        NotificationSubscribeRequest sr =
                new NotificationSubscribeRequest(randomUUID().getMostSignificantBits(), null, null, null);
        Request request = Request.newBuilder()
                .withBody(sr)
                .withPartitionKey(randomUUID().toString())
                .withSingleReply(false)
                .build();
        handler.handle(request);
    }
}

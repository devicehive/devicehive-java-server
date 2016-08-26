package com.devicehive.handler.notification;

import com.devicehive.eventbus.EventBus;
import com.devicehive.handler.NotificationSubscribeRequestHandler;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.Action;
import com.devicehive.model.rpc.NotificationSubscribeRequest;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

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
        this.handler = new NotificationSubscribeRequestHandler();

        this.eventBus = mock(EventBus.class);
        this.hazelcastService = mock(HazelcastService.class);

        this.handler.setEventBus(eventBus);
        this.handler.setHazelcastService(hazelcastService);
    }

    @Test
    public void shouldSubscribeToDeviceNotifications() throws Exception {
        String subscriptionId = randomUUID().toString();
        String device = randomUUID().toString();
        NotificationSubscribeRequest sr =
                new NotificationSubscribeRequest(subscriptionId, device, null, null);
        Request request = Request.newBuilder()
                .withBody(sr)
                .withCorrelationId(randomUUID().toString())
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
        assertEquals(subscription.getType(), Action.NOTIFICATION.name());
        assertEquals(subscription.getGuid(), device);
        assertNull(subscription.getName());

        verifyZeroInteractions(hazelcastService);
    }

    @Test
    public void shouldSubscribeToDeviceNotificationsNames() throws Exception {
        String subscriptionId = randomUUID().toString();
        String device = randomUUID().toString();
        Set<String> names = Stream.of("a", "b", "c").collect(Collectors.toSet());
        NotificationSubscribeRequest sr =
                new NotificationSubscribeRequest(subscriptionId, device, names, null);
        Request request = Request.newBuilder()
                .withBody(sr)
                .withCorrelationId(randomUUID().toString())
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
            assertEquals(subscription.getGuid(), device);
            assertEquals(subscription.getType(), Action.NOTIFICATION.name());
        });
        Set<String> notificationNames = subscriptions.stream().map(Subscription::getName).collect(Collectors.toSet());
        assertThat(notificationNames, hasSize(3));
        assertEquals(notificationNames, names);

        verifyZeroInteractions(hazelcastService);
    }

    @Test
    public void shouldSubscribeAndSearchNotificationsInCache() throws Exception {
        String subscriptionId = randomUUID().toString();
        String device = randomUUID().toString();
        Set<String> names = Stream.of("a", "b", "c").collect(Collectors.toSet());
        Date timestamp = new Date();
        NotificationSubscribeRequest sr =
                new NotificationSubscribeRequest(subscriptionId, device, names, timestamp);
        Request request = Request.newBuilder()
                .withBody(sr)
                .withCorrelationId(randomUUID().toString())
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
            assertEquals(subscription.getGuid(), device);
            assertEquals(subscription.getType(), Action.NOTIFICATION.name());
        });
        Set<String> notificationNames = subscriptions.stream().map(Subscription::getName).collect(Collectors.toSet());
        assertThat(notificationNames, hasSize(3));
        assertEquals(notificationNames, names);

        verify(hazelcastService).find(null, null, Collections.singleton(device), names,
                timestamp, NotificationSubscribeRequestHandler.LIMIT, DeviceNotification.class);
    }

    @Test
    public void shouldThrowIfBodyIsNull() throws Exception {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("Request body is null");

        Request request = Request.newBuilder()
                .withCorrelationId(randomUUID().toString())
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
                .withCorrelationId(randomUUID().toString())
                .withPartitionKey(randomUUID().toString())
                .withSingleReply(false)
                .build();
        handler.handle(request);
    }

    @Test
    public void shouldThrowIfDeviceGuidIsNull() throws Exception {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("Device guid is null");

        NotificationSubscribeRequest sr =
                new NotificationSubscribeRequest(randomUUID().toString(), null, null, null);
        Request request = Request.newBuilder()
                .withBody(sr)
                .withCorrelationId(randomUUID().toString())
                .withPartitionKey(randomUUID().toString())
                .withSingleReply(false)
                .build();
        handler.handle(request);
    }
}

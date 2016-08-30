package com.devicehive.eventbus.test;

import com.devicehive.eventbus.EventBus;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.eventbus.events.NotificationEvent;
import com.devicehive.model.rpc.Action;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.MessageDispatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EventBusTest {

    private EventBus eventBus;
    private MessageDispatcher dispatcher;

    @Before
    public void setUp() throws Exception {
        dispatcher = mock(MessageDispatcher.class);
        eventBus = new EventBus(dispatcher);
    }

    @Test
    public void shouldSubscribeToDeviceNotification() throws Exception {
        String deviceGuid = UUID.randomUUID().toString();
        String subscriberTopic = "reply_topic";

        Subscriber subscriber = new Subscriber(UUID.randomUUID().toString(), subscriberTopic, "correlation_id");
        Subscription subscription = new Subscription(Action.NOTIFICATION_EVENT.name(), deviceGuid);
        eventBus.subscribe(subscriber, subscription);

        DeviceNotification notification = new DeviceNotification();
        notification.setDeviceGuid(deviceGuid);
        notification.setNotification(randomAlphabetic(5));
        notification.setId(0);
        NotificationEvent event = new NotificationEvent(notification);
        eventBus.publish(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        verify(dispatcher).send(topicCaptor.capture(), responseCaptor.capture());
        assertEquals(subscriber.getReplyTo(), topicCaptor.getValue());

        Response response = responseCaptor.getValue();
        assertEquals(subscriber.getCorrelationId(), response.getCorrelationId());
        assertFalse(response.isFailed());
        assertFalse(response.isLast());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof NotificationEvent);

        NotificationEvent sentEvent = (NotificationEvent) response.getBody();
        assertNotNull(sentEvent.getNotification());
        assertEquals(sentEvent.getNotification(), notification);
    }

    @Test
    public void shouldSubscribeToDeviceNotificationWithName() throws Exception {
        String deviceGuid = UUID.randomUUID().toString();

        Subscriber subscriber1 = new Subscriber(UUID.randomUUID().toString(), randomAlphabetic(5), UUID.randomUUID().toString());
        Subscription subscription1 = new Subscription(Action.NOTIFICATION_EVENT.name(), deviceGuid);
        eventBus.subscribe(subscriber1, subscription1);

        Subscriber subscriber2 = new Subscriber(UUID.randomUUID().toString(), randomAlphabetic(5), UUID.randomUUID().toString());
        Subscription subscription2 = new Subscription(Action.NOTIFICATION_EVENT.name(), deviceGuid, "temperature");
        eventBus.subscribe(subscriber2, subscription2);

        Subscriber subscriber3 = new Subscriber(UUID.randomUUID().toString(), randomAlphabetic(5), UUID.randomUUID().toString());
        Subscription subscription3 = new Subscription(Action.NOTIFICATION_EVENT.name(), deviceGuid, "vibration");
        eventBus.subscribe(subscriber3, subscription3);

        Subscriber subscriber4 = new Subscriber(UUID.randomUUID().toString(), randomAlphabetic(5), UUID.randomUUID().toString());
        Subscription subscription4 = new Subscription(Action.COMMAND_EVENT.name(), deviceGuid, "go_offline");
        eventBus.subscribe(subscriber4, subscription4);

        DeviceNotification notification = new DeviceNotification();
        notification.setDeviceGuid(deviceGuid);
        notification.setNotification("temperature");
        notification.setId(0);
        notification.setTimestamp(new Date());
        NotificationEvent notificationEvent = new NotificationEvent(notification);
        eventBus.publish(notificationEvent);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        verify(dispatcher, times(2)).send(topicCaptor.capture(), responseCaptor.capture());
        List<String> topics = topicCaptor.getAllValues();
        assertThat(topics, hasSize(2));
        assertThat(topics, contains(subscriber1.getReplyTo(), subscriber2.getReplyTo()));

        List<Response> responses = responseCaptor.getAllValues();
        assertThat(responses, hasSize(2));
        responses.forEach(response -> {
            assertFalse(response.isLast());
            assertFalse(response.isFailed());
            assertTrue(response.getBody() instanceof NotificationEvent);
        });

        Response response1 = responses.get(0);
        Response response2 = responses.get(1);
        assertEquals(response1.getCorrelationId(), subscriber1.getCorrelationId());
        assertEquals(response2.getCorrelationId(), subscriber2.getCorrelationId());

        assertEquals(response1.getBody(), notificationEvent);
        assertEquals(response2.getBody(), notificationEvent);
    }

    @Test
    public void shouldUnsubscribeFromNotificationEvents() throws Exception {
        String deviceGuid1 = UUID.randomUUID().toString();
        String deviceGuid2 = UUID.randomUUID().toString();

        //subscriber1 subscribes to deviceGuid1 temperature notifications
        Subscriber subscriber1 = new Subscriber(UUID.randomUUID().toString(), randomAlphabetic(5), UUID.randomUUID().toString());
        Subscription subscription1 = new Subscription(Action.NOTIFICATION_EVENT.name(), deviceGuid1, "temperature");
        eventBus.subscribe(subscriber1, subscription1);

        //subscriber1 subscribes to deviceGuid2 temperature notifications
        Subscription subscription2 = new Subscription(Action.NOTIFICATION_EVENT.name(), deviceGuid2, "temperature");
        eventBus.subscribe(subscriber1, subscription2);

        //subscriber2 subscribes to deviceGuid2 temperature notifications
        Subscriber subscriber2 = new Subscriber(UUID.randomUUID().toString(), randomAlphabetic(5), UUID.randomUUID().toString());
        Subscription subscription3 = new Subscription(Action.NOTIFICATION_EVENT.name(), deviceGuid2, "temperature");
        eventBus.subscribe(subscriber2, subscription3);

        //submit notification for deviceGuid1
        DeviceNotification notification = new DeviceNotification();
        notification.setDeviceGuid(deviceGuid1);
        notification.setNotification("temperature");
        notification.setId(0);
        NotificationEvent notificationEvent1 = new NotificationEvent(notification);
        eventBus.publish(notificationEvent1);

        //submit notification for deviceGuid2
        notification = new DeviceNotification();
        notification.setDeviceGuid(deviceGuid2);
        notification.setNotification("temperature");
        notification.setId(0);
        NotificationEvent notificationEvent2 = new NotificationEvent(notification);
        eventBus.publish(notificationEvent2);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        verify(dispatcher, times(3)).send(topicCaptor.capture(), responseCaptor.capture());
        List<String> topics = topicCaptor.getAllValues();
        List<Response> responses = responseCaptor.getAllValues();
        assertThat(topics, hasSize(3));
        assertThat(responses, hasSize(3));
        Set<String> topicsSet = topics.stream().collect(Collectors.toSet());
        assertThat(topicsSet, hasSize(2));
        assertThat(topicsSet, containsInAnyOrder(subscriber1.getReplyTo(), subscriber2.getReplyTo()));
        responses.forEach(r -> {
            assertFalse(r.isLast());
            assertFalse(r.isFailed());
            assertNotNull(r.getBody());
            assertTrue(r.getBody() instanceof NotificationEvent);
        });
        Response response1 = responses.get(0);
        assertEquals(response1.getCorrelationId(), subscriber1.getCorrelationId());
        assertEquals(response1.getBody(), notificationEvent1);
        Response response2 = responses.get(1);
        assertEquals(response2.getCorrelationId(), subscriber1.getCorrelationId());
        assertEquals(response2.getBody(), notificationEvent2);
        Response response3 = responses.get(2);
        assertEquals(response3.getCorrelationId(), subscriber2.getCorrelationId());
        assertEquals(response3.getBody(), notificationEvent2);

        reset(dispatcher);
        eventBus.unsubscribe(subscriber1);

        //submit notification for deviceGuid1
        notification = new DeviceNotification();
        notification.setDeviceGuid(deviceGuid1);
        notification.setNotification("temperature");
        notification.setId(0);
        notificationEvent1 = new NotificationEvent(notification);
        eventBus.publish(notificationEvent1);

        //submit notification for deviceGuid2
        notification = new DeviceNotification();
        notification.setDeviceGuid(deviceGuid2);
        notification.setNotification("temperature");
        notification.setId(0);
        notificationEvent2 = new NotificationEvent(notification);
        eventBus.publish(notificationEvent2);

        topicCaptor = ArgumentCaptor.forClass(String.class);
        responseCaptor = ArgumentCaptor.forClass(Response.class);

        verify(dispatcher).send(topicCaptor.capture(), responseCaptor.capture());
        assertEquals(topicCaptor.getValue(), subscriber2.getReplyTo());
        assertEquals(responseCaptor.getValue(), response3);
    }
}

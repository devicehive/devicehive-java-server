package com.devicehive;

import com.devicehive.base.AbstractSpringTest;
import com.devicehive.base.NotificationTestUtils;
import com.devicehive.eventbus.EventBus;
import com.devicehive.eventbus.FilterRegistry;
import com.devicehive.messages.handler.command.CommandInsertHandler;
import com.devicehive.messages.handler.command.CommandSearchHandler;
import com.devicehive.messages.handler.notification.NotificationInsertHandler;
import com.devicehive.messages.handler.notification.NotificationSubscribeRequestHandler;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.eventbus.events.CommandEvent;
import com.devicehive.model.eventbus.events.NotificationEvent;
import com.devicehive.model.rpc.*;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static com.devicehive.base.CommandTestUtils.generateCommand;
import static com.devicehive.model.enums.SortOrder.DESC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class BackendSmokeTest extends AbstractSpringTest {

    @Autowired
    private HazelcastService hazelcastService;

    @Autowired
    private RpcClient client;

    @Autowired
    private FilterRegistry filterRegistry;

    private EventBus eventBus;

    private CommandInsertHandler commandInsertHandler;
    private CommandSearchHandler commandSearchHandler;

    private NotificationInsertHandler notificationInsertHandler;
    private NotificationSubscribeRequestHandler notificationSubscribeRequestHandler;

    @Rule
    public ExpectedException ex = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        eventBus = Mockito.mock(EventBus.class);

        commandInsertHandler = new CommandInsertHandler();
        commandInsertHandler.setEventBus(eventBus);
        commandInsertHandler.setHazelcastService(hazelcastService);

        commandSearchHandler = new CommandSearchHandler();
        commandSearchHandler.setHazelcastService(hazelcastService);

        notificationInsertHandler = new NotificationInsertHandler();
        notificationInsertHandler.setEventBus(eventBus);
        notificationInsertHandler.setHazelcastService(hazelcastService);

        notificationSubscribeRequestHandler = new NotificationSubscribeRequestHandler();
        notificationSubscribeRequestHandler.setEventBus(eventBus);
        notificationSubscribeRequestHandler.setHazelcastService(hazelcastService);
        notificationSubscribeRequestHandler.setFilterRegistry(filterRegistry);
    }

    @Test
    public void shouldSendNotificationSearchAction() throws Exception {
        NotificationSearchRequest searchRequest = new NotificationSearchRequest();
        searchRequest.setId(Long.MAX_VALUE); // nonexistent id
        searchRequest.setDeviceId(UUID.randomUUID().toString()); // random device id

        Request request = Request.newBuilder()
                .withPartitionKey(searchRequest.getDeviceId())
                .withBody(searchRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        NotificationSearchResponse responseBody = (NotificationSearchResponse) response.getBody();
        assertTrue(responseBody.getNotifications().isEmpty());
    }

    @Test
    public void shouldSendCommandSearchAction() throws Exception {
        CommandSearchRequest searchRequest = new CommandSearchRequest();
        searchRequest.setId(Long.MAX_VALUE); // nonexistent id
        searchRequest.setDeviceId(UUID.randomUUID().toString()); // random device id

        Request request = Request.newBuilder()
                .withPartitionKey(searchRequest.getDeviceId())
                .withBody(searchRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        CommandSearchResponse responseBody = (CommandSearchResponse) response.getBody();
        assertTrue(responseBody.getCommands().isEmpty());
    }

    @Test
    public void shouldSendCommandInsertAction() throws Exception {
        DeviceCommand command = new DeviceCommand();
        command.setCommand("test_command");
        command.setDeviceId(UUID.randomUUID().toString());
        CommandInsertRequest insertRequest = new CommandInsertRequest(command);

        Request request = Request.newBuilder()
                .withPartitionKey(insertRequest.getDeviceCommand().getDeviceId())
                .withBody(insertRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        CommandInsertResponse responseBody = (CommandInsertResponse) response.getBody();
        assertNotNull(responseBody.getDeviceCommand());
    }

    @Test
    public void shouldSendCommandUpdateAction() throws Exception {
        DeviceCommand command = new DeviceCommand();
        command.setCommand("test_command");
        command.setResult(new JsonStringWrapper("{\"result\": \"OK\"}"));
        command.setDeviceId(UUID.randomUUID().toString());
        CommandUpdateRequest updateRequest = new CommandUpdateRequest(command);

        Request request = Request.newBuilder()
                .withPartitionKey(updateRequest.getDeviceCommand().getDeviceId())
                .withBody(updateRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        assertNotNull(response);
    }

    @Test
    public void shouldSendNotificationInsertAction() throws Exception {
        DeviceNotification notification = new DeviceNotification();
        notification.setNotification("test_notification");
        notification.setDeviceId(UUID.randomUUID().toString());
        NotificationInsertRequest insertRequest = new NotificationInsertRequest(notification);

        Request request = Request.newBuilder()
                .withPartitionKey(insertRequest.getDeviceNotification().getDeviceId())
                .withBody(insertRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        NotificationInsertResponse responseBody = (NotificationInsertResponse) response.getBody();
        assertNotNull(responseBody.getDeviceNotification());
    }

    @Test
    public void shouldSendListUserAction() throws Exception {
        ListUserRequest listUserRequest = new ListUserRequest();
        listUserRequest.setLogin(UUID.randomUUID().toString()); // nonexistent login

        Request request = Request.newBuilder()
                .withBody(listUserRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        ListUserResponse responseBody = (ListUserResponse) response.getBody();
        assertNotNull(responseBody.getUsers().isEmpty());
    }

    @Test
    public void shouldSendListNetworkAction() throws Exception {
        ListNetworkRequest listNetworkRequest = new ListNetworkRequest();
        listNetworkRequest.setName(UUID.randomUUID().toString()); // nonexistent name

        Request request = Request.newBuilder()
                .withBody(listNetworkRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        ListNetworkResponse responseBody = (ListNetworkResponse) response.getBody();
        assertNotNull(responseBody.getNetworks().isEmpty());
    }

    @Test
    public void shouldSendListDeviceAction() throws Exception {
        ListDeviceRequest deviceRequest = new ListDeviceRequest();
        deviceRequest.setName(UUID.randomUUID().toString()); // nonexistent name
        deviceRequest.setSortOrder(DESC.name());

        Request request = Request.newBuilder()
                .withBody(deviceRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        ListDeviceResponse responseBody = (ListDeviceResponse) response.getBody();
        assertNotNull(responseBody.getDevices().isEmpty());
    }

    @Test
    public void shouldHandleCommandInsert() throws Exception {
        DeviceCommand command = generateCommand();
        CommandInsertRequest cir = new CommandInsertRequest(command);
        Response response = commandInsertHandler.handle(
                Request.newBuilder()
                        .withBody(cir)
                        .build()
        );
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof CommandInsertResponse);
        CommandInsertResponse body = (CommandInsertResponse) response.getBody();
        assertEquals(body.getDeviceCommand(), command);

        Optional<DeviceCommand> cmd = hazelcastService.find(command.getId(), command.getDeviceId(), DeviceCommand.class);
        assertTrue(cmd.isPresent());
        assertEquals(cmd.get(), command);

        ArgumentCaptor<CommandEvent> eventCaptor = ArgumentCaptor.forClass(CommandEvent.class);
        verify(eventBus).publish(eventCaptor.capture());
        CommandEvent event = eventCaptor.getValue();
        assertEquals(event.getCommand(), command);
    }

    @Test
    public void shouldHandleCommandInsertAndGetByCommandIdAndDeviceId() throws Exception {
        DeviceCommand command = generateCommand();

        CommandInsertRequest cir = new CommandInsertRequest(command);
        Response response = commandInsertHandler.handle(
                Request.newBuilder()
                        .withBody(cir)
                        .build()
        );
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof CommandInsertResponse);
        CommandInsertResponse body = (CommandInsertResponse) response.getBody();
        assertEquals(command, body.getDeviceCommand());

        CommandSearchRequest csr = new CommandSearchRequest();
        csr.setDeviceId(command.getDeviceId());
        csr.setId(command.getId());
        response = commandSearchHandler.handle(
                Request.newBuilder()
                        .withBody(csr)
                        .build()
        );
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof CommandSearchResponse);
        CommandSearchResponse responseBody = (CommandSearchResponse) response.getBody();
        assertEquals(1, responseBody.getCommands().size());
        assertEquals(command, responseBody.getCommands().get(0));
    }

    @Test
    public void shouldHandleNotificationInsert() throws ExecutionException, InterruptedException, TimeoutException {
        final String deviceId = UUID.randomUUID().toString();
        final long id = System.nanoTime();

        DeviceNotification originalNotification = new DeviceNotification();
        originalNotification.setTimestamp(Date.from(Instant.now()));
        originalNotification.setId(id);
        originalNotification.setDeviceId(deviceId);
        originalNotification.setNetworkId(0L);
        originalNotification.setNotification("SOME TEST DATA");
        originalNotification.setParameters(new JsonStringWrapper("{\"param1\":\"value1\",\"param2\":\"value2\"}"));
        NotificationInsertRequest nir = new NotificationInsertRequest(originalNotification);
        Response response = notificationInsertHandler.handle(
                Request.newBuilder()
                        .withBody(nir)
                        .build()
        );

        assertTrue(hazelcastService.find(id, deviceId, DeviceNotification.class)
                .filter(notification -> notification.equals(originalNotification))
                .isPresent());

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventBus).publish(eventCaptor.capture());
        NotificationEvent event = eventCaptor.getValue();
        assertEquals(event.getNotification(), originalNotification);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof NotificationInsertResponse);
        NotificationInsertResponse body = (NotificationInsertResponse) response.getBody();
        assertEquals(body.getDeviceNotification(), originalNotification);
    }

    @Test
    public void shouldFindSingleNotificationByIdAndDeviceId() throws Exception {
        String deviceId = UUID.randomUUID().toString();
        // create notifications
        List<DeviceNotification> notifications = LongStream.range(0, 3)
                .mapToObj(i -> NotificationTestUtils.generateNotification(i, i, deviceId))
                .collect(Collectors.toList());

        // insert notifications
        notifications.stream()
                .map(notification -> NotificationTestUtils.insertNotification(client, notification))
                .forEach(NotificationTestUtils::waitForResponse);

        NotificationSearchRequest searchRequest = new NotificationSearchRequest();
        searchRequest.setId(notifications.get(0).getId());
        searchRequest.setDeviceId(notifications.get(0).getDeviceId());

        Request request = Request.newBuilder()
                .withPartitionKey(notifications.get(0).getDeviceId())
                .withBody(searchRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        NotificationSearchResponse responseBody = (NotificationSearchResponse) response.getBody();
        assertEquals(1, responseBody.getNotifications().size());
        assertEquals(notifications.get(0), responseBody.getNotifications().get(0));
    }

    @Test
    public void shouldHandleNotificationInsertAndQueryByDeviceIdAndNotificationName() throws Exception {
        String deviceId = UUID.randomUUID().toString();
        // create notifications
        List<DeviceNotification> notifications = LongStream.range(0, 3)
                .mapToObj(i -> NotificationTestUtils.generateNotification(i, i, deviceId))
                .collect(Collectors.toList());

        // insert notifications
        notifications.stream()
                .map(notification -> NotificationTestUtils.insertNotification(client, notification))
                .forEach(NotificationTestUtils::waitForResponse);

        NotificationSearchRequest searchRequest = new NotificationSearchRequest();
        searchRequest.setDeviceId(notifications.get(0).getDeviceId());
        searchRequest.setNames(Collections.singleton(notifications.get(0).getNotification()));

        Request request = Request.newBuilder()
                .withBody(searchRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        NotificationSearchResponse responseBody = (NotificationSearchResponse) response.getBody();
        assertEquals(1, responseBody.getNotifications().size());
        assertEquals(notifications.get(0), responseBody.getNotifications().get(0));
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
        notificationSubscribeRequestHandler.handle(request);

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
        notificationSubscribeRequestHandler.handle(request);

        ArgumentCaptor<Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Subscriber.class);
        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(eventBus, times(names.size())).subscribe(subscriberCaptor.capture(), subscriptionCaptor.capture());

        Set<Subscriber> subscribers = new HashSet<>(subscriberCaptor.getAllValues());
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
    }

    @Test
    public void shouldThrowIfBodyIsNull() throws Exception {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("Request body is null");

        Request request = Request.newBuilder()
                .withPartitionKey(randomUUID().toString())
                .withSingleReply(false)
                .build();
        notificationSubscribeRequestHandler.handle(request);
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
        notificationSubscribeRequestHandler.handle(request);
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
        notificationSubscribeRequestHandler.handle(request);
    }
}

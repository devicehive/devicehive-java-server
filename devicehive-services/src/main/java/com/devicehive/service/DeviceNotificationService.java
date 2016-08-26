package com.devicehive.service;

import com.devicehive.dao.DeviceDao;
import com.devicehive.messages.handler.ClientHandler;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.eventbus.events.NotificationEvent;
import com.devicehive.model.rpc.*;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.vo.DeviceVO;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class DeviceNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceNotificationService.class);

    @Autowired
    private DeviceEquipmentService deviceEquipmentService;

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private RpcClient rpcClient;

    public Optional<DeviceNotification> find(Long id, String guid) {
        return find(id, guid, null, null, null, null).stream().findFirst();
    }

    @SuppressWarnings("unchecked")
    public Collection<DeviceNotification> find(Long id, String guid,
                                               Collection<String> devices, Collection<String> names,
                                               Date timestamp, Integer take) {
        Request request = Request.newBuilder()
                .withCorrelationId(UUID.randomUUID().toString())
                .withBody(new NotificationSearchRequest() {{
                    setId(id);
                    setGuid(guid);
                    setDevices(new HashSet<>(devices));
                    setNames(new HashSet<>(names));
                    setTimestamp(timestamp);
                    setTake(take);
                }})
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(request, future::complete);

        try {
            Response response = future.get(10, TimeUnit.SECONDS);
            return ((NotificationSearchResponse) response.getBody()).getNotifications();
        } catch (InterruptedException | ExecutionException e) {
            logger.warn("Unable to find notification due to unexpected exception", e);
        } catch (TimeoutException e) {
            logger.warn("Notification find was timed out (id={}, guid={})", id, guid, e);
        }
        return Collections.emptyList();
    }

    public void submitDeviceNotification(final DeviceNotification notification, final DeviceVO device) {
        processDeviceNotification(notification, device).forEach(n -> {
            rpcClient.push(Request.newBuilder()
                    .withBody(new NotificationInsertRequest(notification))
                    .withPartitionKey(device.getGuid())
                    .build());
        });
    }

    public void submitDeviceNotification(final DeviceNotification notification, final String deviceGuid) {
        notification.setTimestamp(timestampService.getTimestamp());
        notification.setId(Math.abs(new Random().nextInt()));
        notification.setDeviceGuid(deviceGuid);
        rpcClient.push(Request.newBuilder()
                .withBody(new NotificationInsertRequest(notification))
                .withPartitionKey(deviceGuid)
                .build());
    }

    public String submitDeviceSubscribeNotification(final Set<String> devices,
                                                    final Set<String> names,
                                                    final Date timestamp,
                                                    final ClientHandler clientHandler) throws InterruptedException {
        String subscriptionId = UUID.randomUUID().toString();
        Set<NotificationSubscribeRequest> subscribeRequests = devices.stream()
                .map(device -> new NotificationSubscribeRequest(subscriptionId, device, names, timestamp))
                .collect(Collectors.toSet());
        CountDownLatch responseLatch = new CountDownLatch(subscribeRequests.size());
        Set<DeviceNotification> notifications = new HashSet<>();
        for (NotificationSubscribeRequest subscribeRequest : subscribeRequests) {
            Consumer<Response> callback = response -> {
                String resAction = response.getBody().getAction();
                if (resAction.equals(Action.NOTIFICATION_SUBSCRIBE_RESPONSE.name())) {
                    NotificationSubscribeResponse subscribeResponse = (NotificationSubscribeResponse) response.getBody();
                    notifications.addAll(subscribeResponse.getNotifications());
                    responseLatch.countDown();
                } else if (resAction.equals(Action.NOTIFICATION.name())) {
                    NotificationEvent event = (NotificationEvent) response.getBody();
                    JsonObject json = ServerResponsesFactory.createNotificationInsertMessage(event.getNotification(), subscriptionId);
                    clientHandler.sendMessage(json);
                } else {
                    logger.warn("Unknown action received from backend {}", resAction);
                }
            };
            Request request = Request.newBuilder()
                    .withBody(subscribeRequest)
                    .withPartitionKey(subscribeRequest.getDevice())
                    .withCorrelationId(UUID.randomUUID().toString())
                    .withSingleReply(false)
                    .build();
            rpcClient.call(request, callback);
        }
        responseLatch.await();
        if (!notifications.isEmpty()) {
            //todo send existent notifications to device AFTER subscription response
        }
        return subscriptionId;
    }

    public DeviceNotification convertToMessage(DeviceNotificationWrapper notificationSubmit, DeviceVO device) {
        DeviceNotification message = new DeviceNotification();
        message.setId(Math.abs(new Random().nextInt()));
        message.setDeviceGuid(device.getGuid());
        message.setTimestamp(timestampService.getTimestamp());
        message.setNotification(notificationSubmit.getNotification());
        message.setParameters(notificationSubmit.getParameters());
        return message;
    }

    private List<DeviceNotification> processDeviceNotification(DeviceNotification notificationMessage, DeviceVO device) {
        List<DeviceNotification> notificationsToCreate = new ArrayList<>();
        switch (notificationMessage.getNotification()) {
            case SpecialNotifications.EQUIPMENT:
                deviceEquipmentService.refreshDeviceEquipment(notificationMessage, device);
                break;
            case SpecialNotifications.DEVICE_STATUS:
                DeviceNotification deviceNotification = refreshDeviceStatusCase(notificationMessage, device);
                notificationsToCreate.add(deviceNotification);
                break;
            default:
                break;

        }
        notificationsToCreate.add(notificationMessage);
        return notificationsToCreate;

    }

    private DeviceNotification refreshDeviceStatusCase(DeviceNotification notificationMessage, DeviceVO device) {
        DeviceVO devicevo = deviceDao.findByUUID(device.getGuid());
        String status = ServerResponsesFactory.parseNotificationStatus(notificationMessage);
        devicevo.setStatus(status);
        return ServerResponsesFactory.createNotificationForDevice(devicevo, SpecialNotifications.DEVICE_UPDATE);
    }
}

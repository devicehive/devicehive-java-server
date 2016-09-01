package com.devicehive.service;

import com.devicehive.dao.DeviceDao;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.eventbus.events.NotificationEvent;
import com.devicehive.model.rpc.*;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.vo.DeviceVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
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

    public CompletableFuture<Optional<DeviceNotification>> find(Long id, String guid) {
        NotificationSearchRequest searchRequest = new NotificationSearchRequest();
        searchRequest.setId(id);
        searchRequest.setGuid(guid);

        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(searchRequest)
                .withPartitionKey(searchRequest.getGuid())
                .build(), new ResponseConsumer(future));
        return future.thenApply(r -> ((NotificationSearchResponse) r.getBody()).getNotifications().stream().findFirst());
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<List<DeviceNotification>> find(Collection<String> guids, Collection<String> names,
                                                            Date timestamp, Integer take) {
        List<CompletableFuture<Response>> futures = guids.stream()
                .map(guid -> {
                    NotificationSearchRequest searchRequest = new NotificationSearchRequest();
                    searchRequest.setGuid(guid);
                    searchRequest.setNames(new HashSet<>(names));
                    searchRequest.setTimestamp(timestamp);
                    searchRequest.setTake(take);
                    return searchRequest;
                })
                .map(searchRequest -> {
                    CompletableFuture<Response> future = new CompletableFuture<>();
                    rpcClient.call(Request.newBuilder()
                            .withBody(searchRequest)
                            .withPartitionKey(searchRequest.getGuid())
                            .build(), new ResponseConsumer(future));
                    return future;
                })
                .collect(Collectors.toList());

        // List<CompletableFuture<Response>> => CompletableFuture<List<DeviceNotification>>
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)                                            // List<CompletableFuture<Response>> => CompletableFuture<List<Response>>
                        .map(r -> ((NotificationSearchResponse) r.getBody()).getNotifications()) // CompletableFuture<List<Response>> => CompletableFuture<List<List<DeviceNotification>>>
                        .flatMap(Collection::stream)                                             // CompletableFuture<List<List<DeviceNotification>>> => CompletableFuture<List<DeviceNotification>>
                        .collect(Collectors.toList()));
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

    public CompletableFuture<Collection<DeviceNotification>> sendSubscribeRequest(String subscriptionId,
                                                                                  Set<String> devices,
                                                                                  Set<String> names,
                                                                                  Date timestamp,
                                                                                  Consumer<DeviceNotification> callback) {
        Set<NotificationSubscribeRequest> subscribeRequests = devices.stream()
                .map(device -> new NotificationSubscribeRequest(subscriptionId, device, names, timestamp))
                .collect(Collectors.toSet());
        Collection<CompletableFuture<Collection<DeviceNotification>>> futures = new ArrayList<>();
        for (NotificationSubscribeRequest sr : subscribeRequests) {
            CompletableFuture<Collection<DeviceNotification>> future = new CompletableFuture<>();
            Consumer<Response> responseConsumer = response -> {
                String resAction = response.getBody().getAction();
                if (resAction.equals(Action.NOTIFICATION_SUBSCRIBE_RESPONSE.name())) {
                    NotificationSubscribeResponse r = response.getBody().cast(NotificationSubscribeResponse.class);
                    future.complete(r.getNotifications());
                } else if (resAction.equals(Action.NOTIFICATION_EVENT.name())) {
                    NotificationEvent event = response.getBody().cast(NotificationEvent.class);
                    callback.accept(event.getNotification());
                } else {
                    logger.warn("Unknown action received from backend {}", resAction);
                }
            };
            futures.add(future);
            Request request = Request.newBuilder()
                    .withBody(sr)
                    .withPartitionKey(sr.getDevice())
                    .withSingleReply(false)
                    .build();
            rpcClient.call(request, responseConsumer);
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()));
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

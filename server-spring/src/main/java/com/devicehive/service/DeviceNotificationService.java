package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.messages.bus.redis.RedisNotificationService;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.ServerResponsesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.*;

@Component
public class DeviceNotificationService {
    private static final int MAX_NOTIFICATION_COUNT = 100;

    @Autowired
    private DeviceEquipmentService deviceEquipmentService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private TimestampService timestampService;
    @Autowired
    private DeviceDAO deviceDAO;
    @Autowired
    private RedisNotificationService redisNotificationService;
    @Autowired
    private MessageBus messageBus;

    public DeviceNotification findByIdAndGuid(final Long id, final String guid) {
        return redisNotificationService.getByIdAndGuid(id, guid);
    }

    public Collection<DeviceNotification> getDeviceNotificationsList(Collection<String> devices, final Collection<String> names,
                                                     final Timestamp timestamp, final Integer take,
                                                     HivePrincipal principal) {
        Collection<DeviceNotification> notifications;
        if (devices != null) {
            final List<String> availableDevices = deviceService.findGuidsWithPermissionsCheck(devices, principal);
            notifications = redisNotificationService.getByGuids(availableDevices, timestamp, names, take);
        } else {
            notifications = redisNotificationService.getAll(timestamp, names, take);
        }
        if (!CollectionUtils.isEmpty(notifications) && notifications.size() > MAX_NOTIFICATION_COUNT) {
            return new ArrayList<>(notifications).subList(0, MAX_NOTIFICATION_COUNT);
        }
        return notifications;
    }

    public void submitDeviceNotification(final DeviceNotification notification, final Device device) {
        List<DeviceNotification> proceedNotifications = processDeviceNotification(notification, device);
        for (DeviceNotification currentNotification : proceedNotifications) {
            redisNotificationService.save(notification);
            messageBus.publishDeviceNotification(currentNotification);
        }
    }

    public void submitDeviceNotification(final DeviceNotification notification, final String deviceGuid) {
        notification.setTimestamp(timestampService.getTimestamp());
        notification.setId(Math.abs(new Random().nextInt()));
        notification.setDeviceGuid(deviceGuid);
        redisNotificationService.save(notification);
        messageBus.publishDeviceNotification(notification);
    }

    public List<DeviceNotification> processDeviceNotification(DeviceNotification notificationMessage, Device device) {
        List<DeviceNotification> notificationsToCreate = new ArrayList<>();
        switch (notificationMessage.getNotification()) {
            case SpecialNotifications.EQUIPMENT:
                deviceEquipmentService.refreshDeviceEquipment(notificationMessage, device);
                break;
            case SpecialNotifications.DEVICE_STATUS:
                notificationsToCreate.add(refreshDeviceStatusCase(notificationMessage, device));
                break;
            default:
                break;

        }
        notificationMessage.setDeviceGuid(device.getGuid());
        notificationMessage.setTimestamp(timestampService.getTimestamp());
        notificationsToCreate.add(notificationMessage);
        return notificationsToCreate;

    }

    public DeviceNotification convertToMessage(DeviceNotificationWrapper notificationSubmit, Device device) {
        DeviceNotification message = new DeviceNotification();
        message.setId(Math.abs(new Random().nextInt()));
        message.setDeviceGuid(device.getGuid());
        message.setTimestamp(timestampService.getTimestamp());
        message.setNotification(notificationSubmit.getNotification());
        message.setParameters(notificationSubmit.getParameters());
        return message;
    }

    public DeviceNotification refreshDeviceStatusCase(DeviceNotification notificationMessage, Device device) {
        device = deviceDAO.findByUUIDWithNetworkAndDeviceClass(device.getGuid());
        String status = ServerResponsesFactory.parseNotificationStatus(notificationMessage);
        device.setStatus(status);
        return ServerResponsesFactory.createNotificationForDevice(device, SpecialNotifications.DEVICE_UPDATE);
    }

    public void submitEmptyResponse(final AsyncResponse asyncResponse) {
        asyncResponse.resume(ResponseFactory.response(Response.Status.OK, Collections.emptyList(),
                JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT));
    }
}

package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.messages.bus.hazelcast.HazelcastNotificationHelper;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.service.time.HazelcastTimestampService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.ServerResponsesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.util.*;

@Component
public class DeviceNotificationService {
    @Autowired
    private DeviceEquipmentService deviceEquipmentService;
    @Autowired
    private TimestampService timestampService;
    @Autowired
    private DeviceDAO deviceDAO;
    @Autowired
    private MessageBus messageBus;
    @Autowired
    private HazelcastNotificationHelper notificationHelper;

    public DeviceNotification findByIdAndGuid(final Long id, final String guid) {
        final List<DeviceNotification> notifications =
                new ArrayList<>(getDeviceNotificationsList(id, guid, null, null, null, 1, null));
        return notifications.isEmpty() ? null : notifications.get(0);
    }

    public Collection<DeviceNotification> getDeviceNotificationsList(final Long id, final String guid,
                                                                     final Collection<String> devices,
                                                                     final Collection<String> names,
                                                                     final Timestamp timestamp, final Integer take,
                                                                     HivePrincipal principal) {
        final Map<String, Comparable []> filters = notificationHelper.prepareFilters(id, guid, devices, names, timestamp, principal);
        return notificationHelper.retrieve(filters, take);
    }

    public void submitDeviceNotification(final DeviceNotification notification, final Device device) {
        List<DeviceNotification> proceedNotifications = processDeviceNotification(notification, device);
        for (DeviceNotification currentNotification : proceedNotifications) {
            notificationHelper.store(notification);
            messageBus.publishDeviceNotification(currentNotification);
        }
    }

    public void submitDeviceNotification(final DeviceNotification notification, final String deviceGuid) {
        notification.setTimestamp(timestampService.getTimestamp());
        notification.setId(Math.abs(new Random().nextInt()));
        notification.setDeviceGuid(deviceGuid);
        notificationHelper.store(notification);
        messageBus.publishDeviceNotification(notification);
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

    private List<DeviceNotification> processDeviceNotification(DeviceNotification notificationMessage, Device device) {
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
        notificationsToCreate.add(notificationMessage);
        return notificationsToCreate;

    }
    private DeviceNotification refreshDeviceStatusCase(DeviceNotification notificationMessage, Device device) {
        device = deviceDAO.findByUUIDWithNetworkAndDeviceClass(device.getGuid());
        String status = ServerResponsesFactory.parseNotificationStatus(notificationMessage);
        device.setStatus(status);
        return ServerResponsesFactory.createNotificationForDevice(device, SpecialNotifications.DEVICE_UPDATE);
    }
}

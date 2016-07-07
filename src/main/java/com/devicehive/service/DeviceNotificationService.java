package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.rdbms.GenericDaoImpl;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.ServerResponsesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DeviceNotificationService extends AbstractHazelcastEntityService {
    @Autowired
    private DeviceEquipmentService deviceEquipmentService;
    @Autowired
    private TimestampService timestampService;
    @Autowired
    private GenericDaoImpl genericDAO;

    public DeviceNotification find(Long id, String guid) {
        return find(id, guid, DeviceNotification.class);
    }

    public Collection<DeviceNotification> find(Long id, String guid, Collection<String> devices,
                                               Collection<String> names,
                                               Date timestamp, Integer take, HivePrincipal principal) {

        return find(id, guid, devices, names, timestamp, take, principal, DeviceNotification.class);
    }

    public void submitDeviceNotification(final DeviceNotification notification, final Device device) {
        List<DeviceNotification> proceedNotifications = processDeviceNotification(notification, device);
        for (DeviceNotification currentNotification : proceedNotifications) {
            store(currentNotification, DeviceNotification.class);
        }
    }

    public void submitDeviceNotification(final DeviceNotification notification, final String deviceGuid) {
        notification.setTimestamp(timestampService.getTimestamp());
        notification.setId(Math.abs(new Random().nextInt()));
        notification.setDeviceGuid(deviceGuid);
        store(notification, DeviceNotification.class);
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
        device = genericDAO.createNamedQuery(Device.class, "Device.findByUUID", Optional.of(CacheConfig.refresh()))
                .setParameter("guid", device.getGuid())
                .getResultList()
                .stream().findFirst().orElse(null);
        String status = ServerResponsesFactory.parseNotificationStatus(notificationMessage);
        device.setStatus(status);
        return ServerResponsesFactory.createNotificationForDevice(device, SpecialNotifications.DEVICE_UPDATE);
    }
}

package com.devicehive.service;

import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.messages.bus.GlobalMessageBus;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.User;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.utils.ServerResponsesFactory;
import com.devicehive.utils.Timer;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Stateless
@LogExecutionTime
public class DeviceNotificationService {

    @EJB
    private DeviceNotificationDAO deviceNotificationDAO;
    @EJB
    private TimestampService timestampService;
    @EJB
    private DeviceEquipmentService deviceEquipmentService;
    @EJB
    private GlobalMessageBus globalMessageBus;
    @EJB
    private DeviceNotificationService self;

    @EJB
    private DeviceDAO deviceDAO;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceNotification> getDeviceNotificationList(List<Device> deviceList, User user, Timestamp timestamp,
                                                              Boolean isAdmin) {
        if (deviceList == null) {
            if (isAdmin) {
                return deviceNotificationDAO.findNewerThan(timestamp);
            } else {
                return deviceNotificationDAO.getByUserNewerThan(user, timestamp);
            }
        }
        if (deviceList.isEmpty()) {
            return null;
        }
        return deviceNotificationDAO.findByDevicesNewerThan(deviceList, timestamp);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceNotification findById(@NotNull long id) {
        return deviceNotificationDAO.findById(id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceNotification> queryDeviceNotification(Device device, Timestamp start, Timestamp end,
                                                            String notification,
                                                            String sortField, Boolean sortOrderAsc, Integer take,
                                                            Integer skip) {
        return deviceNotificationDAO
                .queryDeviceNotification(device, start, end, notification, sortField, sortOrderAsc, take, skip);
    }


    //device should be already set

    public List<DeviceNotification> saveDeviceNotification(List<DeviceNotification> notifications) {
        for (DeviceNotification notification : notifications) {
            deviceNotificationDAO.createNotification(notification);
        }
        return notifications;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void submitDeviceNotification(DeviceNotification notification, Device device) {
        Timer timer = Timer.newInstance();
        List<DeviceNotification> proceedNotifications = self.processDeviceNotification(notification, device);
        timer.logMethodExecuted("DeviceNotificationService.self.processDeviceNotification");
        for (DeviceNotification currentNotification : proceedNotifications) {
            globalMessageBus.publishDeviceNotification(currentNotification);
        }
    }

    public List<DeviceNotification> processDeviceNotification(DeviceNotification notification, Device device) {
        List<DeviceNotification> notificationsToCreate = new ArrayList<>();
        switch (notification.getNotification()) {
            case SpecialNotifications.EQUIPMENT:
                deviceEquipmentService.refreshDeviceEquipment(notification, device);
                break;
            case SpecialNotifications.DEVICE_STATUS:
                notificationsToCreate.add(refreshDeviceStatusCase(notification, device));
                break;
            default:
                break;

        }
        notification.setDevice(device);
        notificationsToCreate.add(notification);
        return saveDeviceNotification(notificationsToCreate);

    }


    public DeviceNotification refreshDeviceStatusCase(DeviceNotification notification, Device device) {
        device = deviceDAO.findByUUIDWithNetworkAndDeviceClass(device.getGuid());
        String status = ServerResponsesFactory.parseNotificationStatus(notification);
        device.setStatus(status);
        return ServerResponsesFactory.createNotificationForDevice(device, SpecialNotifications.DEVICE_UPDATE);
    }


}

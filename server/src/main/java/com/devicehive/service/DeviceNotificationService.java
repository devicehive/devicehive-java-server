package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.bus.GlobalMessageBus;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ServerResponsesFactory;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Stateless
@LogExecutionTime
public class DeviceNotificationService {
    @EJB
    private DeviceNotificationDAO deviceNotificationDAO;
    @EJB
    private DeviceEquipmentService deviceEquipmentService;
    @EJB
    private DeviceDAO deviceDAO;
    @EJB
    private DeviceService deviceService;
    @Inject
    private Event<DeviceNotification> event;


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceNotification> getDeviceNotificationList(Collection<String> devices, Collection<String> names,
                                                              Timestamp timestamp,
                                                              HivePrincipal principal) {
        if (devices != null) {
            List<Device> availableDevices = deviceService.findByGuidWithPermissionsCheck(devices, principal);
            if (availableDevices.size() != devices.size()) {
                Set<String> availableDeviceIds = new HashSet<>();
                for (Device currentDevice : availableDevices) {
                    availableDeviceIds.add(currentDevice.getGuid());
                }
                Set<String> notAllowedDeviceIds = new HashSet<>();
                for (String guid : devices) {
                    if (!availableDeviceIds.contains(guid))
                        notAllowedDeviceIds.add(guid);
                }
                String message = String.format(Messages.DEVICES_NOT_FOUND, StringUtils.join(notAllowedDeviceIds, ","));
                throw new HiveException(message, Response.Status.NOT_FOUND.getStatusCode());
            }
            return deviceNotificationDAO.findNotifications(availableDevices, names, timestamp, null);
        } else {
            return deviceNotificationDAO.findNotifications(null, names, timestamp, principal);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceNotification findById(@NotNull long id) {
        return deviceNotificationDAO.findById(id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceNotification> queryDeviceNotification(Device device,
                                                            Timestamp start,
                                                            Timestamp end,
                                                            String notification,
                                                            String sortField,
                                                            Boolean sortOrderAsc,
                                                            Integer take,
                                                            Integer skip,
                                                            Integer gridInterval) {
        return deviceNotificationDAO
                .queryDeviceNotification(device, start, end, notification, sortField, sortOrderAsc, take, skip,
                        gridInterval);
    }


    //device should be already set

    public List<DeviceNotification> saveDeviceNotification(List<DeviceNotification> notifications) {
        for (DeviceNotification notification : notifications) {
            deviceNotificationDAO.createNotification(notification);
        }
        return notifications;
    }

    public void submitDeviceNotification(DeviceNotification notification, Device device) {
        List<DeviceNotification> proceedNotifications = processDeviceNotification(notification, device);
        for (DeviceNotification currentNotification : proceedNotifications) {
            event.fire(currentNotification);
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

package com.devicehive.service;

import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;

import javax.ejb.EJB;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;

public class DeviceNotificationService {

    @EJB
    private DeviceNotificationDAO deviceNotificationDAO;

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

    public DeviceNotification findById(@NotNull long id) {
        return deviceNotificationDAO.findById(id);
    }

    public List<DeviceNotification> queryDeviceNotification(Device device, Timestamp start, Timestamp end,
                                                            String notification,
                                                            String sortField, Boolean sortOrderAsc, Integer take,
                                                            Integer skip) {
        return deviceNotificationDAO.queryDeviceNotification(device, start, end, notification, sortField, sortOrderAsc, take, skip);
    }


}

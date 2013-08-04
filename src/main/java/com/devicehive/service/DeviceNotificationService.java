package com.devicehive.service;

import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.List;

public class DeviceNotificationService {

    @Inject
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

}

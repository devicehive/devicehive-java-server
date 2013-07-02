package com.devicehive.service;

import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class DeviceNotificationService {
    @Inject
    private DeviceNotificationDAO deviceNotificationDAO;

    public List<DeviceNotification> getDeviceNotificationList(List<Device> deviceList, Date timestamp){
        if (deviceList.isEmpty()){
            deviceNotificationDAO.findNewerThan(timestamp);
        }
        return deviceNotificationDAO.findByDevicesNewerThan(deviceList,timestamp);
    }

}

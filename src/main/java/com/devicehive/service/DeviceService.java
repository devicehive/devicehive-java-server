package com.devicehive.service;

import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;
import com.devicehive.websockets.messagebus.global.MessagePublisher;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 21.06.13
 * Time: 12:52
 * To change this template use File | Settings | File Templates.
 */
public class DeviceService {

    @Inject
    private DeviceCommandDAO deviceCommandDAO;

    @Inject
    private DeviceNotificationDAO deviceNotificationDAO;

    @Inject
    private MessagePublisher messagePublisher;



    @Transactional
    public void submitDeviceCommand(DeviceCommand command, Device device, User user) {
        command.setDevice(device);
        command.setUser(user);
        command.setTimestamp(new Date());
        deviceCommandDAO.saveCommand(command);
        messagePublisher.publishCommand(command);
    }


    @Transactional
    public void submitDeviceCommandUpdate(DeviceCommand update, Device device) {
        update.setDevice(device);
        deviceCommandDAO.updateCommand(update);
        messagePublisher.publishCommand(update);
    }


    @Transactional
    public void submitDeviceNotification(DeviceNotification notification, Device device) {
        notification.setDevice(device);
        deviceNotificationDAO.saveNotification(notification);
        messagePublisher.publishNotification(notification);
    }
}

package com.devicehive.service;

import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;
import com.devicehive.websockets.messagebus.global.MessagePublisher;
import com.devicehive.websockets.messagebus.local.LocalMessageBus;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.websocket.Session;
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

    @Inject
    private LocalMessageBus localMessageBus;

    @Inject
    private UserDAO userDAO;



    @Transactional
    public void submitDeviceCommand(DeviceCommand command, Device device, User user, Session userWebsocketSession) {
        command.setDevice(device);
        command.setUser(user);
        command.setTimestamp(new Date());
        deviceCommandDAO.saveCommand(command);
        if (userWebsocketSession != null) {
            localMessageBus.subscribeForCommandUpdates(command.getId(), userWebsocketSession);
        }
        messagePublisher.publishCommand(command);
    }

    @Transactional
    public void submitDeviceCommand(DeviceCommand command, Device device, User user) {
        submitDeviceCommand(command, device, user, null);
    }


    @Transactional
    public void submitDeviceCommandUpdate(DeviceCommand update, Device device) {
        deviceCommandDAO.updateCommand(update, device);
        messagePublisher.publishCommandUpdate(update);
    }


    @Transactional
    public void submitDeviceNotification(DeviceNotification notification, Device device) {
        notification.setDevice(device);
        deviceNotificationDAO.saveNotification(notification);
        //TODO implement device_equipment notifications
        messagePublisher.publishNotification(notification);
    }
}

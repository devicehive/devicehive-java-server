package com.devicehive.service;

import com.devicehive.dao.*;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.websockets.messagebus.global.MessagePublisher;
import com.devicehive.websockets.messagebus.local.LocalMessageBus;
import com.devicehive.websockets.messagebus.local.subscriptions.dao.CommandUpdatesSubscriptionDAO;
import com.devicehive.websockets.messagebus.local.subscriptions.model.CommandUpdatesSubscription;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.websocket.Session;
import java.util.Date;
import java.util.Set;


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
    private DeviceClassDAO deviceClassDAO;
    @Inject
    private UserDAO userDAO;
    @Inject
    private DeviceDAO deviceDAO;
    @Inject
    private EquipmentDAO equipmentDAO;
    @Inject
    private NetworkService networkService;
    @Inject
    private EquipmentService equipmentService;
    @Inject
    private CommandUpdatesSubscriptionDAO commandUpdatesSubscriptionDAO;

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
    public void submitDeviceCommandUpdate(DeviceCommand update, Device device, Session session) {
        deviceCommandDAO.updateCommand(update, device);
        messagePublisher.publishCommandUpdate(update);
        commandUpdatesSubscriptionDAO.insert(new CommandUpdatesSubscription(device.getId(), session.getId()));
    }

    @Transactional
    public void submitDeviceNotification(DeviceNotification notification, Device device) {
        notification.setDevice(device);
        deviceNotificationDAO.saveNotification(notification);
        //TODO implement device_equipment notifications
        messagePublisher.publishNotification(notification);
    }

    @Transactional
    public void updateDevice(Device device, Set<Equipment> equipmentSet) {
        resolveNetworkAndDeviceClassAndEquipment(device, equipmentSet);
        deviceDAO.updateDevice(device);
    }

    @Transactional
    public void registerDevice(Device device, Set<Equipment> equipmentSet) {
        resolveNetworkAndDeviceClassAndEquipment(device, equipmentSet);
        deviceDAO.registerDevice(device);
    }

    private void resolveNetworkAndDeviceClassAndEquipment(Device device, Set<Equipment> equipmentSet) {
        DeviceClass deviceClass = device.getDeviceClass();
        Network networkFromMessage = device.getNetwork();
        DeviceClass resultDeviceClass = getResultDeviceClass(deviceClass);
        device.setDeviceClass(resultDeviceClass);
        if (networkFromMessage != null) {
            device.setNetwork(networkService.getNetwork(networkFromMessage));
        }
        if (!resultDeviceClass.getPermanent() && equipmentSet != null && !equipmentSet.isEmpty()) {
            resolveEquipment(resultDeviceClass, equipmentSet);
        }
    }

    private void resolveEquipment(DeviceClass deviceClass, Set<Equipment> equipmentSet) {

        for (Equipment equipment : equipmentSet) {
            equipment.setDeviceClass(deviceClass);
        }
        if (equipmentService.validateEquipments(equipmentSet)) {
            equipmentDAO.removeUnusefulEquipments(deviceClass, equipmentSet);
            equipmentDAO.saveOrUpdateEquipments(equipmentSet);
        }


    }

    private DeviceClass getResultDeviceClass(DeviceClass deviceClassFromMessage) {
        DeviceClass deviceClass;
        if (deviceClassFromMessage.getId() != null) {
            deviceClass = deviceClassDAO.getDeviceClass(deviceClassFromMessage.getId());
        } else {
            deviceClass = deviceClassDAO.getDeviceClassByNameAndVersion(deviceClassFromMessage
                    .getName(), deviceClassFromMessage.getVersion());
        }
        if (deviceClass == null) {
            createDeviceClass(deviceClassFromMessage);
            deviceClass = deviceClassFromMessage;
        } else {
            deviceClass = updateDeviceClassIfRequired(deviceClass, deviceClassFromMessage);
        }
        return deviceClass;
    }

    private DeviceClass updateDeviceClassIfRequired(DeviceClass deviceClassFromDatabase,
                                                    DeviceClass deviceClassFromMessage) {
        if (deviceClassFromDatabase.getPermanent()) {
            return deviceClassFromDatabase;
        }
        ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
        Validator validator = vf.getValidator();
        Set<String> validationErrorsSet = DeviceClass.validate(deviceClassFromMessage, validator);
        if (validationErrorsSet.isEmpty()) {
            boolean updateClass = false;   //equals + exclusion isPermanent?
            if (deviceClassFromMessage.getName() != null && !deviceClassFromMessage.getName().equals
                    (deviceClassFromDatabase.getName())) {
                deviceClassFromDatabase.setName(deviceClassFromMessage.getName());
                updateClass = true;
            }
            if (deviceClassFromMessage.getVersion() != null && !deviceClassFromMessage.getVersion().equals
                    (deviceClassFromDatabase.getVersion())) {
                deviceClassFromDatabase.setVersion(deviceClassFromMessage.getVersion());
                updateClass = true;
            }
            if (deviceClassFromMessage.getOfflineTimeout() != null && !deviceClassFromMessage.getOfflineTimeout()
                    .equals(deviceClassFromDatabase.getOfflineTimeout())) {
                deviceClassFromDatabase.setOfflineTimeout(deviceClassFromMessage.getOfflineTimeout());
                updateClass = true;
            }
            if (deviceClassFromMessage.getData() != null && !deviceClassFromMessage.getData().equals
                    (deviceClassFromDatabase.getData())) {
                deviceClassFromDatabase.setData(deviceClassFromMessage.getData());
                updateClass = true;
            }
            if (updateClass) {
                deviceClassDAO.updateDeviceClass(deviceClassFromDatabase);
            }
        } else {
            String exceptionMessage = "Validation faild: ";
            for (String violation : validationErrorsSet) {
                exceptionMessage += violation + "\n";
            }
            throw new HiveException(exceptionMessage);
        }
        return deviceClassFromDatabase;
    }

    @Transactional
    public void createDeviceClass(DeviceClass deviceClass) {
        ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
        Validator validator = vf.getValidator();
        Set<String> validationErrorsSet = DeviceClass.validate(deviceClass, validator);
        if (validationErrorsSet.isEmpty()) {
            deviceClassDAO.addDeviceClass(deviceClass);
        } else {
            String exceptionMessage = "Validation faild: ";
            for (String violation : validationErrorsSet) {
                exceptionMessage += violation + "\n";
            }
            throw new HiveException(exceptionMessage);
        }
    }


}

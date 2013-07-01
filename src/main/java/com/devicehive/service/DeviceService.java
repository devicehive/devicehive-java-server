package com.devicehive.service;

import com.devicehive.dao.*;
import com.devicehive.model.*;
import com.devicehive.websockets.json.GsonFactory;
import com.devicehive.websockets.messagebus.global.MessagePublisher;
import com.devicehive.websockets.messagebus.local.LocalMessageBus;
import com.devicehive.websockets.messagebus.local.subscriptions.dao.CommandUpdatesSubscriptionDAO;
import com.devicehive.websockets.messagebus.local.subscriptions.model.CommandUpdatesSubscription;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.websocket.Session;
import java.util.Date;
import java.util.Set;
import java.util.UUID;


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
    private NetworkService networkService;
    @Inject
    private EquipmentService equipmentService;
    @Inject
    private DeviceEquipmentService deviceEquipmentService;
    @Inject
    private CommandUpdatesSubscriptionDAO commandUpdatesSubscriptionDAO;

    @Transactional
    public void deviceSave(Device device, Set<Equipment> equipmentSet, UUID deviceId){
        Device existingDevice = deviceDAO.findByUUID(deviceId);
        if (existingDevice != null) {
            existingDevice.setName(device.getName());
            existingDevice.setData(device.getData());
            existingDevice.setStatus(device.getStatus());
            existingDevice.setKey(device.getKey());
            updateDevice(existingDevice, device.getNetwork(), device.getDeviceClass(), equipmentSet);
        } else {
            device.setGuid(deviceId);
            registerDevice(device, device.getNetwork(), device.getDeviceClass(), equipmentSet);
        }
    }

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
        if (notification.getNotification().equals("equipment")){
            //TODO implement device_equipment notifications
            String jsonParametersString = notification.getParameters().getJsonString();
            Gson gson = GsonFactory.createGson();
            JsonObject jsonEquipmentObject = gson.fromJson(jsonParametersString, JsonObject.class);
            DeviceEquipment deviceEquipment = constructDeviceEquipmentObject(jsonEquipmentObject, device);
            deviceEquipmentService.resolveSaveOrUpdateEquipment(deviceEquipment);
        }
        messagePublisher.publishNotification(notification);
    }

    @Transactional
    public void updateDevice(Device device, Network network, DeviceClass deviceClass, Set<Equipment> equipmentSet) {
        resolveNetworkAndDeviceClassAndEquipment(device, network, deviceClass, equipmentSet);
        deviceDAO.updateDevice(device);
    }

    @Transactional
    public void registerDevice(Device device, Network network, DeviceClass deviceClass, Set<Equipment> equipmentSet) {
        resolveNetworkAndDeviceClassAndEquipment(device, network, deviceClass, equipmentSet);
        deviceDAO.registerDevice(device);
    }

    private void resolveNetworkAndDeviceClassAndEquipment(Device device, Network networkFromMessage,
                                                          DeviceClass deviceClass,
                                                          Set<Equipment> equipmentSet) {

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
        equipmentService.removeUnusefulEquipments(deviceClass, equipmentSet);
        equipmentService.saveOrUpdateEquipments(equipmentSet);


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

        return deviceClassFromDatabase;
    }

    @Transactional
    public void createDeviceClass(DeviceClass deviceClass) {
        deviceClassDAO.addDeviceClass(deviceClass);
    }

    private DeviceEquipment constructDeviceEquipmentObject (JsonObject jsonEquipmentObject, Device device){
        DeviceEquipment result = new DeviceEquipment();
        String deviceEquipmentCode = jsonEquipmentObject.get("equipment").getAsString();
        result.setCode(deviceEquipmentCode);
        //TODO getParameters?
        jsonEquipmentObject.remove("equipment");
        result.setParameters(new JsonStringWrapper(jsonEquipmentObject.toString()));
        result.setDevice(device);
        return result;
    }


}

package com.devicehive.service;

import com.devicehive.dao.*;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.websockets.json.GsonFactory;
import com.devicehive.websockets.messagebus.global.MessagePublisher;
import com.devicehive.websockets.messagebus.local.LocalMessageBus;
import com.devicehive.websockets.messagebus.local.subscriptions.dao.CommandUpdatesSubscriptionDAO;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.websocket.Session;
import java.util.Date;
import java.util.Set;
import java.util.UUID;


public class DeviceService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
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
    public void deviceSave(Device device, Set<Equipment> equipmentSet, UUID deviceId) {
        Device existingDevice = deviceDAO.findByUUID(deviceId);
        if (existingDevice != null) {
            logger.debug("device with uuid = " + device.getGuid() + "exists. Device will be updated");
            existingDevice.setName(device.getName());
            existingDevice.setData(device.getData());
            existingDevice.setStatus(device.getStatus());
            existingDevice.setKey(device.getKey());
            updateDevice(existingDevice, device.getNetwork(), device.getDeviceClass(), equipmentSet);
        } else {
            logger.debug("device with uuid = " + deviceId + "doesn't exists. Device will be saved");
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
    }

    public void submitDeviceNotification(DeviceNotification notification, Device device) {
        DeviceEquipment deviceEquipment = null;
        if (notification.getNotification().equals("equipment")) {
            deviceEquipment = parseNotification(notification, device);
        }
        submitDeviceNotificationTransactionProcess(notification, device, deviceEquipment);

    }

    private DeviceEquipment parseNotification(DeviceNotification notification, Device device) {
        String jsonParametersString = notification.getParameters().getJsonString();
        Gson gson = GsonFactory.createGson();
        JsonElement parametersJsonElement = gson.fromJson(jsonParametersString, JsonElement.class);
        JsonObject jsonEquipmentObject;
        if (parametersJsonElement instanceof JsonObject) {
            jsonEquipmentObject = (JsonObject) parametersJsonElement;
        } else {
            throw new HiveException("\"parameters\" must be JSON Object!");
        }
        return constructDeviceEquipmentObject(jsonEquipmentObject, device);
    }

    @Transactional
    public void submitDeviceNotificationTransactionProcess(DeviceNotification notification, Device device,
                                                           DeviceEquipment deviceEquipment) {
        if (deviceEquipment != null) {
            deviceEquipmentService.resolveSaveOrUpdateEquipment(deviceEquipment);
        }
        notification.setDevice(device);
        deviceNotificationDAO.saveNotification(notification);
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
            device.setNetwork(networkService.createOrVeriryNetwork(networkFromMessage));
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

    private DeviceEquipment constructDeviceEquipmentObject(JsonObject jsonEquipmentObject, Device device) {
        DeviceEquipment result = new DeviceEquipment();
        String deviceEquipmentCode = jsonEquipmentObject.get("equipment").getAsString();
        result.setCode(deviceEquipmentCode);
        jsonEquipmentObject.remove("equipment");
        result.setParameters(new JsonStringWrapper(jsonEquipmentObject.toString()));
        result.setDevice(device);
        return result;
    }


}

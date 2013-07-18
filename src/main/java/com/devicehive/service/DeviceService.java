package com.devicehive.service;

import com.devicehive.dao.*;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.websockets.json.GsonFactory;
import com.devicehive.websockets.messagebus.global.MessagePublisher;
import com.devicehive.websockets.messagebus.local.LocalMessageBus;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.websocket.Session;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Stateless
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
    private EquipmentDAO equipmentDAO;
    @Inject
    private NetworkService networkService;
    @Inject
    private DeviceEquipmentDAO deviceEquipmentDAO;

    public void deviceSave(Device device, Set<Equipment> equipmentSet) {
            device.setNetwork(networkService.createOrVeriryNetwork(device.getNetwork()));
            device.setDeviceClass(createOrUpdateDeviceClass(device.getDeviceClass(), equipmentSet));
            createOrUpdateDevice(device);
    }

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

    public void submitDeviceCommandUpdate(DeviceCommand update, Device device) {
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

    public void submitDeviceNotificationTransactionProcess(DeviceNotification notification, Device device,
                                                           DeviceEquipment deviceEquipment) {
        if (deviceEquipment != null) {
            if (deviceEquipmentDAO.update(deviceEquipment) == 0) {
                deviceEquipment.setTimestamp(new Date());
                deviceEquipmentDAO.saveDeviceEquipment(deviceEquipment);
            }
        }
        notification.setDevice(device);
        deviceNotificationDAO.saveNotification(notification);
        messagePublisher.publishNotification(notification);

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

    public DeviceClass createOrUpdateDeviceClass(DeviceClass deviceClass, Set<Equipment> newEquipmentSet) {
        DeviceClass stored;
        if (deviceClass.getId() != null) {
            stored = deviceClassDAO.getDeviceClass(deviceClass.getId());
        } else {
            stored = deviceClassDAO.getDeviceClassByNameAndVersion(deviceClass.getName(),
                    deviceClass.getVersion());
        }
        if (stored != null) {
            //update
            if (!stored.getPermanent()) {
                stored.setData(deviceClass.getData());
                stored.setOfflineTimeout(deviceClass.getOfflineTimeout());
                stored.setPermanent(deviceClass.getPermanent());
                updateEquipment(newEquipmentSet, stored);
            }
            return stored;
        } else {
            //create
            deviceClassDAO.saveDeviceClass(deviceClass);
            updateEquipment(newEquipmentSet, deviceClass);
            return deviceClass;
        }

    }

    public void updateEquipment(Set<Equipment> newEquipmentSet, DeviceClass deviceClass) {
        List<Equipment> existingEquipments = equipmentDAO.getByDeviceClass(deviceClass);
        if (!newEquipmentSet.isEmpty() && !existingEquipments.isEmpty()) {
                equipmentDAO.removeEquipment(existingEquipments);
        }
        for (Equipment equipment : newEquipmentSet) {
            equipment.setDeviceClass(deviceClass);
            equipmentDAO.saveEquipment(equipment);
        }
    }

    public void createOrUpdateDevice(Device device) {
        Device existingDevice = deviceDAO.findByUUID(device.getGuid());
        if (existingDevice == null) {
            deviceDAO.saveDevice(device);
        } else {
            existingDevice.setDeviceClass(device.getDeviceClass());
            existingDevice.setStatus(device.getStatus());
            existingDevice.setData(device.getData());
            existingDevice.setNetwork(device.getNetwork());
        }

    }
}
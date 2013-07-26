package com.devicehive.service;

import com.devicehive.dao.*;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.messages.MessageType;
import com.devicehive.messages.bus.MessageBroadcaster;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.messages.bus.StatefulMessageListener;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.Equipment;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.websocket.Session;
import java.sql.Timestamp;
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
    private MessageBroadcaster messagePublisher;
    @Inject
    private MessageBus messageBus;
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
        if (device.getNetwork() != null) {
            device.setNetwork(networkService.createOrVeriryNetwork(device.getNetwork()));
        }
        device.setDeviceClass(createOrUpdateDeviceClass(device.getDeviceClass(), equipmentSet));
        createOrUpdateDevice(device);
    }

    public void submitDeviceCommand(DeviceCommand command, Device device, User user, Session session) {
        command.setDevice(device);
        command.setUser(user);
        command.setTimestamp(new Timestamp(System.currentTimeMillis()));
        deviceCommandDAO.createCommand(command);
        messagePublisher.addMessageListener(
                new StatefulMessageListener(MessageType.CLIENT_TO_DEVICE_COMMAND, messageBus));
        messagePublisher.publish(command);
    }

    public void submitDeviceCommandUpdate(DeviceCommand update, Device device) {
        deviceCommandDAO.updateCommand(update, device);
        messagePublisher.addMessageListener(
                new StatefulMessageListener(MessageType.DEVICE_TO_CLIENT_UPDATE_COMMAND, messageBus));
        messagePublisher.publish(update);
    }

    public void submitDeviceNotification(DeviceNotification notification, Device device, Session session) {
        DeviceEquipment deviceEquipment = null;
        if (notification.getNotification().equals("equipment")) {
            deviceEquipment = parseNotification(notification, device);
        }

        if (deviceEquipment != null) {
            if (!deviceEquipmentDAO.update(deviceEquipment)) {
                deviceEquipment.setTimestamp(new Timestamp(System.currentTimeMillis()));
                deviceEquipmentDAO.createDeviceEquipment(deviceEquipment);
            }
        }
        notification.setDevice(device);
        deviceNotificationDAO.createNotification(notification);
        messagePublisher
                .addMessageListener(new StatefulMessageListener(MessageType.DEVICE_TO_CLIENT_NOTIFICATION, messageBus));
        messagePublisher.publish(notification);
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
            deviceClassDAO.createDeviceClass(deviceClass);
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
            equipmentDAO.createEquipment(equipment);
        }
    }

    public void createOrUpdateDevice(Device device) {
        Device existingDevice = deviceDAO.findByUUID(device.getGuid());
        if (existingDevice == null) {
            deviceDAO.createDevice(device);
        } else {
            existingDevice.setDeviceClass(device.getDeviceClass());
            existingDevice.setStatus(device.getStatus());
            existingDevice.setData(device.getData());
            existingDevice.setNetwork(device.getNetwork());
            existingDevice.setKey(device.getKey());
        }

    }

    public void checkDevice(Device device) throws HiveException {
        if (device == null) {
            throw new HiveException("Device is empty");
        }
        if (device.getName() == null) {
            throw new HiveException("Device name is empty");
        }
        if (device.getKey() == null) {
            throw new HiveException("Device key is empty");
        }
        if (device.getDeviceClass() == null) {
            throw new HiveException("Device class is empty");
        }
    }
}
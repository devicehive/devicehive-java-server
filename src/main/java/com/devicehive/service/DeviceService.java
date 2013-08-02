package com.devicehive.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.dao.DeviceClassDAO;
import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.DeviceEquipmentDAO;
import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.dao.EquipmentDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.MessageType;
import com.devicehive.messages.bus.MessageBroadcaster;
import com.devicehive.messages.bus.StatefulMessageListener;
import com.devicehive.messages.bus.notify.StatefulNotifier;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.Equipment;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.User;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
    private StatefulNotifier statefulNotifier;
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

    public void deviceSave(DeviceUpdate device, Set<Equipment> equipmentSet, boolean useExistingEquipment) {
        Device deviceToUpdate = device.convertTo();

        deviceToUpdate
                .setNetwork(networkService.createOrVeriryNetwork(device.getNetwork(), device.getGuid().getValue()));
        deviceToUpdate.setDeviceClass(createOrUpdateDeviceClass(device.getDeviceClass(), equipmentSet,
                device.getGuid().getValue(), useExistingEquipment));
        createOrUpdateDevice(deviceToUpdate, device);
    }

    public void submitDeviceCommand(DeviceCommand command, Device device, User user, Session session) {
        command.setDevice(device);
        command.setUser(user);
        command.setTimestamp(new Timestamp(System.currentTimeMillis()));
        deviceCommandDAO.createCommand(command);
        messagePublisher.addMessageListener(
                new StatefulMessageListener(MessageType.CLIENT_TO_DEVICE_COMMAND, statefulNotifier));
        messagePublisher.publish(MessageType.CLIENT_TO_DEVICE_COMMAND, command);
    }

    public Device findByGuid(UUID guid) {
        return deviceDAO.findByUUID(guid);
    }

    public void submitDeviceCommandUpdate(DeviceCommand update, Device device) {
        deviceCommandDAO.updateCommand(update, device);
        messagePublisher.addMessageListener(
                new StatefulMessageListener(MessageType.DEVICE_TO_CLIENT_UPDATE_COMMAND, statefulNotifier));
        messagePublisher.publish(MessageType.DEVICE_TO_CLIENT_UPDATE_COMMAND, update);
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
        messagePublisher.addMessageListener(new StatefulMessageListener(MessageType.DEVICE_TO_CLIENT_NOTIFICATION, statefulNotifier));
        messagePublisher.publish(MessageType.DEVICE_TO_CLIENT_NOTIFICATION, notification);
    }

    private DeviceEquipment parseNotification(DeviceNotification notification, Device device) {
        String jsonParametersString = notification.getParameters().getJsonString();
        Gson gson = GsonFactory.createGson();
        JsonElement parametersJsonElement = gson.fromJson(jsonParametersString, JsonElement.class);
        JsonObject jsonEquipmentObject;
        if (parametersJsonElement instanceof JsonObject) {
            jsonEquipmentObject = (JsonObject) parametersJsonElement;
        }
        else {
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

    public DeviceClass createOrUpdateDeviceClass(NullableWrapper<DeviceClassUpdate> deviceClass,
            Set<Equipment> newEquipmentSet, UUID guid,
            boolean useExistingEquipment) {
        DeviceClass stored;
        //use existing
        if (deviceClass == null) {
            return deviceClassDAO.getByDevice(guid);
        }
        //check is already done
        DeviceClass deviceClassFromMessage = deviceClass.getValue().convertTo();
        if (deviceClassFromMessage.getId() != null) {
            stored = deviceClassDAO.getDeviceClass(deviceClassFromMessage.getId());
        }
        else {
            stored = deviceClassDAO.getDeviceClassByNameAndVersion(deviceClassFromMessage.getName(),
                    deviceClassFromMessage.getVersion());
        }
        if (stored != null) {
            //update
            if (!stored.getPermanent()) {
                if (deviceClass.getValue().getData() != null) {
                    stored.setData(deviceClassFromMessage.getData());
                }
                if (deviceClass.getValue().getOfflineTimeout() != null) {
                    stored.setOfflineTimeout(deviceClassFromMessage.getOfflineTimeout());
                }
                if (deviceClass.getValue().getPermanent() != null) {
                    stored.setPermanent(deviceClassFromMessage.getPermanent());
                }
                if (!useExistingEquipment) {
                    updateEquipment(newEquipmentSet, stored);
                }
            }
            return stored;
        }
        else {
            //create
            deviceClassDAO.createDeviceClass(deviceClassFromMessage);
            if (!useExistingEquipment) {
                updateEquipment(newEquipmentSet, deviceClassFromMessage);
            }
            return deviceClassFromMessage;
        }

    }

    public void updateEquipment(Set<Equipment> newEquipmentSet, DeviceClass deviceClass) {
        List<Equipment> existingEquipments = equipmentDAO.getByDeviceClass(deviceClass);
        if (!newEquipmentSet.isEmpty() && !existingEquipments.isEmpty()) {
            equipmentDAO.delete(existingEquipments);
        }
        for (Equipment equipment : newEquipmentSet) {
            equipment.setDeviceClass(deviceClass);
            equipmentDAO.create(equipment);
        }
    }

    public void createOrUpdateDevice(Device device, DeviceUpdate deviceUpdate) {
        Device existingDevice = deviceDAO.findByUUID(device.getGuid());
        DeviceNotification notification = new DeviceNotification();
        if (existingDevice == null) {
            existingDevice = deviceDAO.createDevice(device);
            notification.setNotification(SpecialNotifications.DEVICE_ADD);
        }
        else {
            existingDevice.setDeviceClass(device.getDeviceClass());
            if (deviceUpdate.getStatus() != null) {
                existingDevice.setStatus(device.getStatus());
            }
            if (deviceUpdate.getData() != null) {
                existingDevice.setData(device.getData());
            }
            if (deviceUpdate.getNetwork() != null) {
                existingDevice.setNetwork(device.getNetwork());
            }
            if (deviceUpdate.getName() != null) {
                existingDevice.setName(device.getName());
            }
            if (deviceUpdate.getKey() != null) {
                existingDevice.setKey(device.getKey());
            }
            deviceDAO.updateDevice(existingDevice.getId(), existingDevice);
            notification.setNotification(SpecialNotifications.DEVICE_UPDATE);
        }
        notification.setDevice(existingDevice);
        Gson gson = GsonFactory.createGson(JsonPolicyDef.Policy.DEVICE_PUBLISHED);
        JsonElement deviceAsJson = gson.toJsonTree(existingDevice);
        JsonStringWrapper wrapperOverDevice = new JsonStringWrapper(deviceAsJson.toString());
        notification.setParameters(wrapperOverDevice);
        submitDeviceNotification(notification, existingDevice, null);
    }

    /**
     * Implementation for model:
     * if field exists and null - error
     * if field does not exists - use field from database
     *
     * @param device
     * @throws HiveException
     */
    public void checkDevice(DeviceUpdate device) throws HiveException {
        if (device == null) {
            throw new HiveException("Device is empty");
        }
        if (device.getName() != null && device.getName().getValue() == null) {
            throw new HiveException("Device name is empty");
        }
        if (device.getKey() != null && device.getKey().getValue() == null) {
            throw new HiveException("Device key is empty");
        }
        if (device.getDeviceClass() != null && device.getDeviceClass().getValue() == null) {
            throw new HiveException("Device class is empty");
        }
    }
}
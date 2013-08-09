package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.*;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.bus.GlobalMessageBus;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.CommandUpdateSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.websockets.util.AsyncMessageDeliverer;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.websocket.Session;
import javax.ws.rs.BadRequestException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Stateless
public class DeviceService {


    @EJB
    private DeviceCommandDAO deviceCommandDAO;

    @EJB
    private DeviceNotificationDAO deviceNotificationDAO;

    @EJB
    private DeviceClassDAO deviceClassDAO;

    @EJB
    private DeviceDAO deviceDAO;

    @EJB
    private EquipmentDAO equipmentDAO;

    @EJB
    private NetworkService networkService;

    @EJB
    private DeviceEquipmentDAO deviceEquipmentDAO;

    @EJB
    private UserDAO userDAO;

    @EJB
    private TimestampService timestampService;

    @EJB
    private GlobalMessageBus globalMessageBus;


    @EJB
    private AsyncMessageDeliverer asyncMessageDeliverer;

    @EJB
    private SubscriptionManager subscriptionManager;

    public void deviceSave(DeviceUpdate device, Set<Equipment> equipmentSet, boolean useExistingEquipment,
                           boolean isAllowedToUpdate) {
        Device deviceToUpdate = device.convertTo();

        deviceToUpdate.setNetwork(networkService.createOrVeriryNetwork(device.getNetwork(), device.getGuid().getValue()));
        deviceToUpdate.setDeviceClass(createOrUpdateDeviceClass(device.getDeviceClass(), equipmentSet,
                device.getGuid().getValue(), useExistingEquipment));
        createOrUpdateDevice(deviceToUpdate, device, isAllowedToUpdate);
    }

    public void submitDeviceCommand(DeviceCommand command, Device device, User user, final Session session) {
        command.setDevice(device);
        command.setUser(user);
        command.setTimestamp(timestampService.getTimestamp());
        deviceCommandDAO.createCommand(command);
        if (session != null) {
            CommandUpdateSubscription commandUpdateSubscription =
                    new CommandUpdateSubscription(command.getId(), session.getId(),
                            new WebsocketHandlerCreator(session, WebsocketSession.COMMAND_UPDATES_SUBSCRIPTION_LOCK, asyncMessageDeliverer));
            subscriptionManager.getCommandUpdateSubscriptionStorage().insert(commandUpdateSubscription);
        }
        globalMessageBus.publishDeviceCommand(command);
    }

    public Device findByUUID(UUID uuid, User u) {
        if (u.isAdmin()) {
            return deviceDAO.findByUUID(uuid);
        } else {
            return deviceDAO.findByUUID(uuid, u.getId());
        }
    }

    public List<Device> findByUUID(List<UUID> list) {
        if (list.size() == 0) {
            return new ArrayList<>(0);
        }
        return deviceDAO.findByUUID(list);
    }

    public List<Device> findByUUIDListAndUser(User user, List<UUID> list) {
        if (list.size() == 0) {
            return new ArrayList<>(0);
        }
        return deviceDAO.findByUUIDListAndUser(user, list);
    }


    public Device getDevice(UUID deviceId, HivePrincipal principal) {

        Device device = deviceDAO.findByUUID(deviceId);

        if (device == null || !checkPermissions(device, principal)) {
            throw new HiveException("Device Not found", 404);
        }
        return device;
    }

    public boolean submitDeviceCommandUpdate(DeviceCommandUpdate update, Device device) {
        DeviceCommand cmd = deviceCommandDAO.findById(update.getId());
        if (cmd == null) {
            throw new HiveException("Command not found!");
        }

        if (!cmd.getDevice().getId().equals(device.getId())) {
            throw new HiveException("Device tries to update incorrect command");
        }
        DeviceCommand result = update.convertTo();
        if (update.getCommand() == null) {
            result.setCommand(cmd.getCommand());
        }
        if (update.getFlags() == null){
            result.setFlags(cmd.getFlags());
        }
        if (update.getLifetime() == null){
            result.setLifetime(cmd.getLifetime());
        }
        if (update.getParameters() == null){
            result.setParameters(cmd.getParameters());
        }
        if (update.getResult() == null){
            result.setResult(cmd.getResult());
        }
        if (update.getStatus() == null){
            result.setStatus(cmd.getStatus());
        }
        if (update.getTimestamp() == null){
            result.setTimestamp(cmd.getTimestamp());
        }
        if (!deviceCommandDAO.updateCommand(result.getId(), result)){
            return false;
        }
        globalMessageBus.publishDeviceCommandUpdate(result);
        return true;
    }

    public void submitDeviceNotification(DeviceNotification notification, Device device, Session session) {
        DeviceEquipment deviceEquipment = null;
        Timestamp ts = timestampService.getTimestamp();
        if (notification.getNotification().equals("equipment")) {
            deviceEquipment = parseNotification(notification, device);
            if (deviceEquipment.getTimestamp() == null) {
                deviceEquipment.setTimestamp(ts);
            }
        }
        if (deviceEquipment != null && !deviceEquipmentDAO.update(deviceEquipment)) {
            deviceEquipment.setTimestamp(ts);
            deviceEquipmentDAO.createDeviceEquipment(deviceEquipment);
        }
        notification.setTimestamp(ts);
        notification.setDevice(device);
        deviceNotificationDAO.createNotification(notification);
        globalMessageBus.publishDeviceNotification(notification);
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

    public DeviceClass createOrUpdateDeviceClass(NullableWrapper<DeviceClassUpdate> deviceClass,
                                                 Set<Equipment> newEquipmentSet, UUID guid, boolean useExistingEquipment) {
        DeviceClass stored;
        //use existing
        if (deviceClass == null) {
            return deviceClassDAO.getByDevice(guid);
        }
        //check is already done
        DeviceClass deviceClassFromMessage = deviceClass.getValue().convertTo();
        if (deviceClassFromMessage.getId() != null) {
            stored = deviceClassDAO.getDeviceClass(deviceClassFromMessage.getId());
        } else {
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
        } else {
            //create
            if (deviceClassFromMessage.getId() != null) {
                throw new HiveException("Invalid request");
            }
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

    public void createOrUpdateDevice(Device device, DeviceUpdate deviceUpdate, boolean isAllowedToUpdate) {
        Device existingDevice = deviceDAO.findByUUID(device.getGuid());
        DeviceNotification notification = new DeviceNotification();
        if (existingDevice == null) {
            if (device.getName() == null || device.getName().equals("")) {
                throw new HiveException("Invalid request parameters.");
            }
            if (device.getDeviceClass() == null) {
                throw new HiveException("Invalid request parameters.");
            }
            existingDevice = deviceDAO.createDevice(device);
            notification.setNotification(SpecialNotifications.DEVICE_ADD);
        } else {
            if (!isAllowedToUpdate) {
                throw new HiveException("Unauthorized. No permissions to update device", 401);
            }
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

    public boolean checkPermissions(Device device, HivePrincipal principal) {
        if (principal.getDevice() != null) {
            if (!device.getGuid().equals(principal.getDevice().getGuid())) {
                return false;
            }
            if (device.getNetwork() == null) {
                return false;
            }
        } else {
            User user = principal.getUser();
            if (user.getRole().equals(UserRole.CLIENT)) {
                User userWithNetworks = userDAO.findUserWithNetworks(user.getId());
                Set<Network> networkSet = userWithNetworks.getNetworks();
                for (Network network : networkSet) {
                    if (network.getId().equals(device.getNetwork().getId())) {
                        return true;
                    }
                }
                return false;
            }
        }
        return true;
    }

    public boolean deleteDevice(@NotNull UUID guid) {
        return deviceDAO.deleteDevice(guid);
    }

    public List<Device> getList(String name,
                                String namePattern,
                                String status,
                                Long networkId,
                                String networkName,
                                Long deviceClassId,
                                String deviceClassName,
                                String deviceClassVersion,
                                String sortField,
                                Boolean sortOrderAsc,
                                Integer take,
                                Integer skip,
                                UserRole currentUserRole,
                                Set<Network> allowedNetworks) {
        return deviceDAO.getList(name, namePattern, status, networkId, networkName, deviceClassId, deviceClassName,
                deviceClassVersion, sortField, sortOrderAsc, take, skip, currentUserRole, allowedNetworks);
    }

}
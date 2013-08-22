package com.devicehive.service;

import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.bus.GlobalMessageBus;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.CommandUpdateSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Stateless
@LogExecutionTime
@EJB(beanInterface = DeviceService.class, name = "DeviceService")
public class DeviceService {
    @EJB
    private DeviceCommandDAO deviceCommandDAO;
    @EJB
    private DeviceNotificationService deviceNotificationService;
    @EJB
    private DeviceDAO deviceDAO;
    @EJB
    private NetworkService networkService;
    @EJB
    private UserDAO userDAO;
    @EJB
    private DeviceClassService deviceClassService;
    @EJB
    private TimestampService timestampService;
    @EJB
    private GlobalMessageBus globalMessageBus;
    @EJB
    private AsyncMessageSupplier asyncMessageDeliverer;
    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private DeviceService self;
    @EJB
    private DeviceActivityService deviceActivityService;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void deviceSaveAndNotify(DeviceUpdate device, Set<Equipment> equipmentSet, boolean useExistingEquipment,
                                    boolean isAllowedToUpdate) {
        DeviceNotification dn = self.deviceSave(device, equipmentSet, useExistingEquipment, isAllowedToUpdate);
        globalMessageBus.publishDeviceNotification(dn);
        deviceActivityService.update(dn.getDevice().getId());
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DeviceNotification deviceSave(DeviceUpdate device, Set<Equipment> equipmentSet, boolean useExistingEquipment,
                                         boolean isAllowedToUpdate) {
        Network network = networkService.createOrVeriryNetwork(device.getNetwork(), device.getGuid().getValue());
        DeviceClass deviceClass = deviceClassService.createOrUpdateDeviceClass(device.getDeviceClass(),
                equipmentSet, device.getGuid().getValue(), useExistingEquipment);
        return createOrUpdateDevice(device, network, deviceClass, isAllowedToUpdate);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void submitDeviceCommand(DeviceCommand command, Device device, User user, final Session session) {
        self.saveDeviceCommand(command, device, user, session);
        globalMessageBus.publishDeviceCommand(command);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void saveDeviceCommand(DeviceCommand command, Device device, User user, final Session session) {
        command.setDevice(device);
        command.setUser(user);
        deviceCommandDAO.createCommand(command);
        if (session != null) {
            CommandUpdateSubscription commandUpdateSubscription =
                    new CommandUpdateSubscription(command.getId(), session.getId(),
                            new WebsocketHandlerCreator(session, WebsocketSession.COMMAND_UPDATES_SUBSCRIPTION_LOCK,
                                    asyncMessageDeliverer));
            subscriptionManager.getCommandUpdateSubscriptionStorage().insert(commandUpdateSubscription);
        }
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

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void submitDeviceCommandUpdate(DeviceCommandUpdate update, Device device) {
        DeviceCommand saved = self.saveDeviceCommandUpdate(update, device);
        globalMessageBus.publishDeviceCommandUpdate(saved);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DeviceCommand saveDeviceCommandUpdate(DeviceCommandUpdate update, Device device) {

        DeviceCommand cmd = deviceCommandDAO.findById(update.getId());

        if (cmd == null) {
            throw new HiveException("Command not found!", NOT_FOUND.getStatusCode());
        }

        if (!cmd.getDevice().getId().equals(device.getId())) {
            throw new HiveException("Device tries to update incorrect command");
        }


        if (update.getCommand() != null) {
            cmd.setCommand(update.getCommand().getValue());
        }
        if (update.getFlags() != null) {
            cmd.setFlags(update.getFlags().getValue());
        }
        if (update.getLifetime() != null) {
            cmd.setLifetime(update.getLifetime().getValue());
        }
        if (update.getParameters() != null) {
            cmd.setParameters(update.getParameters().getValue());
        }
        if (update.getResult() != null) {
            cmd.setResult(update.getResult().getValue());
        }
        if (update.getStatus() != null) {
            cmd.setStatus(update.getStatus().getValue());
        }
        if (update.getTimestamp() != null) {
            cmd.setTimestamp(update.getTimestamp().getValue());
        }
        return cmd;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void submitDeviceNotification(DeviceNotification notification, Device device) {
        DeviceNotification dn = saveDeviceNotification(notification, device);
        globalMessageBus.publishDeviceNotification(dn);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public DeviceNotification saveDeviceNotification(DeviceNotification notification, Device device) {
        if (notification.getNotification().equals(SpecialNotifications.EQUIPMENT)) {
            deviceNotificationService.saveDeviceNotificationEquipmentCase(notification, device);
        } else if (notification.getNotification().equals(SpecialNotifications.DEVICE_STATUS)) {
            self.saveDeviceNotificationStatusCase(notification, device);
        }
        return deviceNotificationService.saveDeviceNotificationOtherCase(notification, device);
    }



    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public DeviceNotification saveDeviceNotificationStatusCase(DeviceNotification notification, Device device) {
        DeviceUpdate deviceUpdate = new DeviceUpdate();
        String status = deviceNotificationService.parseNotificationStatus(notification);
        deviceUpdate.setStatus(new NullableWrapper<>(status));
        device.setStatus(status);
        DeviceNotification updateDeviceNotification = self.createOrUpdateDevice(deviceUpdate, null, null, true);
        globalMessageBus.publishDeviceNotification(updateDeviceNotification);
        return notification;
    }

    public DeviceNotification createOrUpdateDevice(DeviceUpdate deviceUpdate, Network network, DeviceClass deviceClass,
                                                   boolean isAllowedToUpdate) {
        Device existingDevice = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceUpdate.getGuid().getValue());
        DeviceNotification notification = new DeviceNotification();
        if (existingDevice == null) {
            Device device = deviceUpdate.convertTo();
            device.setNetwork(network);
            device.setDeviceClass(deviceClass);
            existingDevice = deviceDAO.createDevice(device);
            notification.setNotification(SpecialNotifications.DEVICE_ADD);
        } else {
            if (!isAllowedToUpdate) {
                throw new HiveException("Unauthorized. No permissions to update device", 401);
            }
            if (deviceUpdate.getDeviceClass() != null) {
                existingDevice.setDeviceClass(deviceClass);
            }
            if (deviceUpdate.getStatus() != null) {
                existingDevice.setStatus(deviceUpdate.getStatus().getValue());
            }
            if (deviceUpdate.getData() != null) {
                existingDevice.setData(deviceUpdate.getData().getValue());
            }
            if (deviceUpdate.getNetwork() != null) {
                existingDevice.setNetwork(network);
            }
            if (deviceUpdate.getName() != null) {
                existingDevice.setName(deviceUpdate.getName().getValue());
            }
            if (deviceUpdate.getKey() != null) {
                existingDevice.setKey(deviceUpdate.getKey().getValue());
            }
            notification.setNotification(SpecialNotifications.DEVICE_UPDATE);
        }
        notification.setDevice(existingDevice);
        Gson gson = GsonFactory.createGson(JsonPolicyDef.Policy.DEVICE_PUBLISHED);
        JsonElement deviceAsJson = gson.toJsonTree(existingDevice);
        JsonStringWrapper wrapperOverDevice = new JsonStringWrapper(deviceAsJson.toString());
        notification.setParameters(wrapperOverDevice);
        return saveDeviceNotification(notification, existingDevice);
    }

    /**
     * Implementation for model:
     * if field exists and null - error
     * if field does not exists - use field from database
     *
     * @param device device to check
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

    public Device getDeviceWithNetworkAndDeviceClass(UUID deviceId, User currentUser, Device currentDevice) {

        if (!checkPermissions(deviceId, currentUser, currentDevice)) {
            throw new HiveException("Device Not found", NOT_FOUND.getStatusCode());
        }

        Device device = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceId);

        if (device == null) {
            throw new HiveException("Device Not found", NOT_FOUND.getStatusCode());
        }
        return device;
    }

    public Device getDevice(UUID deviceId, User currentUser, Device currentDevice) {

        if (!checkPermissions(deviceId, currentUser, currentDevice)) {
            throw new HiveException("Device Not found", NOT_FOUND.getStatusCode());
        }

        Device device = deviceDAO.findByUUID(deviceId);

        if (device == null) {
            throw new HiveException("Device Not found", NOT_FOUND.getStatusCode());
        }
        return device;
    }

    public boolean checkPermissions(Device device, User currentUser, Device currentDevice) {
        if (currentDevice != null) {
            return device.getGuid().equals(currentDevice.getGuid());
        } else {
            if (currentUser.getRole().equals(UserRole.CLIENT)) {
                return userDAO.hasAccessToDevice(currentUser, device);
            }
        }
        return true;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device authenticate(UUID uuid, String key) {
        Device device = deviceDAO.findByUUIDAndKey(uuid, key);
        if (device != null) {
            deviceActivityService.update(device.getId());
        }
        return device;
    }

    public boolean checkPermissions(UUID deviceId, User currentUser, Device currentDevice) {
        if (currentDevice != null) {
            return deviceId.equals(currentDevice.getGuid());
        } else {
            if (currentUser.getRole().equals(UserRole.CLIENT)) {
                return userDAO.hasAccessToDevice(currentUser, deviceId);
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
                                User user) {

        return deviceDAO.getList(name, namePattern, status, networkId, networkName, deviceClassId, deviceClassName,
                deviceClassVersion, sortField, sortOrderAsc, take, skip, user);
    }

}
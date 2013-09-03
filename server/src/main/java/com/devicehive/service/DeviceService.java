package com.devicehive.service;

import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.bus.GlobalMessageBus;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.UserRole;
import com.devicehive.model.domain.*;
import com.devicehive.model.view.DeviceNotificationView;
import com.devicehive.model.view.DeviceView;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.utils.ServerResponsesFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Stateless
@LogExecutionTime
@EJB(beanInterface = DeviceService.class, name = "DeviceService")
public class DeviceService {
    @EJB
    private DeviceNotificationService deviceNotificationService;
    @EJB
    private DeviceEquipmentService deviceEquipmentService;
    @EJB
    private DeviceDAO deviceDAO;
    @EJB
    private NetworkService networkService;
    @EJB
    private UserService userService;
    @EJB
    private DeviceClassService deviceClassService;
    @EJB
    private TimestampService timestampService;
    @EJB
    private GlobalMessageBus globalMessageBus;
    @EJB
    private DeviceService self;
    @EJB
    private DeviceActivityService deviceActivityService;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void deviceSaveAndNotify(DeviceView device, Set<Equipment> equipmentSet, boolean useExistingEquipment,
                                    boolean isAllowedToUpdate) {
        DeviceNotification dn = self.deviceSave(device, equipmentSet, useExistingEquipment, isAllowedToUpdate);
        globalMessageBus.publishDeviceNotification(new DeviceNotificationView(dn));
        deviceActivityService.update(dn.getDevice().getId());
    }


    public DeviceNotification deviceSave(DeviceView deviceView, Set<Equipment> equipmentSet,
                                         boolean useExistingEquipment,
                                         boolean isAllowedToUpdate) {
        Network network = networkService.createOrVeriryNetwork(deviceView.getNetwork());
        DeviceClass deviceClass = deviceClassService
                .createOrUpdateDeviceClass(deviceView.getDeviceClass(), equipmentSet, useExistingEquipment);

        Device existingDevice = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceView.getId());

        if (existingDevice == null) {
            Device device = deviceView.convertTo();
            device.setNetwork(network);
            device.setDeviceClass(deviceClass);
            existingDevice = deviceDAO.createDevice(device);
            final DeviceNotification addDeviceNotification = ServerResponsesFactory.createNotificationForDevice
                    (existingDevice,
                            SpecialNotifications.DEVICE_ADD);
            List<DeviceNotification> resultList =
                    deviceNotificationService.saveDeviceNotification(Arrays.asList(addDeviceNotification));
            return resultList.get(0);
        } else {
            if (!isAllowedToUpdate) {
                throw new HiveException("Unauthorized. No permissions to update device", 401);
            }
            if (deviceView.getDeviceClass() != null && !existingDevice.getDeviceClass().getPermanent()) {
                existingDevice.setDeviceClass(deviceClass);
            }
            if (deviceView.getStatus() != null) {
                existingDevice.setStatus(deviceView.getStatus().getValue());
            }
            if (deviceView.getData() != null) {
                existingDevice.setData(deviceView.getData().getValue());
            }
            if (deviceView.getNetwork() != null) {
                existingDevice.setNetwork(network);
            }
            if (deviceView.getName() != null) {
                existingDevice.setName(deviceView.getName().getValue());
            }
            if (deviceView.getKey() != null) {
                existingDevice.setKey(deviceView.getKey().getValue());
            }
            final DeviceNotification addDeviceNotification = ServerResponsesFactory.createNotificationForDevice
                    (existingDevice,
                            SpecialNotifications.DEVICE_UPDATE);
            List<DeviceNotification> resultList =
                    deviceNotificationService.saveDeviceNotification(Arrays.asList(addDeviceNotification));
            return resultList.get(0);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUID(String uuid, User u) {
        if (u.isAdmin()) {
            return deviceDAO.findByUUID(uuid);
        } else {
            return deviceDAO.findByUUID(uuid, u.getId());
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Device> findByUUID(List<String> list) {
        if (list.size() == 0) {
            return new ArrayList<>(0);
        }
        return deviceDAO.findByUUID(list);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Device> findByUUIDListAndUser(User user, List<String> list) {
        if (list.size() == 0) {
            return new ArrayList<>(0);
        }
        return deviceDAO.findByUUIDListAndUser(user, list);
    }


    /**
     * Implementation for model:
     * if field exists and null - error
     * if field does not exists - use field from database
     *
     * @param device device to check
     * @throws HiveException
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void checkDevice(DeviceView device) throws HiveException {
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

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device getDeviceWithNetworkAndDeviceClass(String deviceId, User currentUser, Device currentDevice) {

        if (!userService.checkPermissions(deviceId, currentUser, currentDevice)) {
            throw new HiveException("Device Not found", NOT_FOUND.getStatusCode());
        }

        Device device = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceId);

        if (device == null) {
            throw new HiveException("Device Not found", NOT_FOUND.getStatusCode());
        }
        return device;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device getDevice(String deviceId, User currentUser, Device currentDevice) {

        if (!userService.checkPermissions(deviceId, currentUser, currentDevice)) {
            throw new HiveException("Device Not found", NOT_FOUND.getStatusCode());
        }

        Device device = deviceDAO.findByUUID(deviceId);

        if (device == null) {
            throw new HiveException("Device Not found", NOT_FOUND.getStatusCode());
        }
        return device;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean checkPermissions(Device device, User currentUser, Device currentDevice) {
        if (currentDevice != null) {
            return device.getGuid().equals(currentDevice.getGuid());
        } else {
            if (currentUser.getRole().equals(UserRole.CLIENT)) {
                return userService.hasAccessToDevice(currentUser, device);
            }
        }
        return true;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device authenticate(String uuid, String key) {
        Device device = deviceDAO.findByUUIDAndKey(uuid, key);
        if (device != null) {
            deviceActivityService.update(device.getId());
        }
        return device;
    }

    public boolean deleteDevice(@NotNull String guid) {
        return deviceDAO.deleteDevice(guid);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
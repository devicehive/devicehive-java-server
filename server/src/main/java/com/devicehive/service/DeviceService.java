package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.filter.AccessKeyBasedFilter;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.bus.GlobalMessageBus;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.utils.ServerResponsesFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import java.util.*;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

@Stateless
@LogExecutionTime
@EJB(beanInterface = DeviceService.class, name = "DeviceService")
public class DeviceService {
    private DeviceNotificationService deviceNotificationService;
    private DeviceDAO deviceDAO;
    private NetworkService networkService;
    private UserService userService;
    private DeviceClassService deviceClassService;
    private GlobalMessageBus globalMessageBus;
    private DeviceService self;
    private DeviceActivityService deviceActivityService;
    private AccessKeyService accessKeyService;

    @EJB
    public void setDeviceNotificationService(DeviceNotificationService deviceNotificationService) {
        this.deviceNotificationService = deviceNotificationService;
    }

    @EJB
    public void setDeviceDAO(DeviceDAO deviceDAO) {
        this.deviceDAO = deviceDAO;
    }

    @EJB
    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    @EJB
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @EJB
    public void setDeviceClassService(DeviceClassService deviceClassService) {
        this.deviceClassService = deviceClassService;
    }

    @EJB
    public void setGlobalMessageBus(GlobalMessageBus globalMessageBus) {
        this.globalMessageBus = globalMessageBus;
    }

    @EJB
    public void setSelf(DeviceService self) {
        this.self = self;
    }

    @EJB
    public void setAccessKeyService(AccessKeyService accessKeyService) {
        this.accessKeyService = accessKeyService;
    }

    @EJB
    public void setDeviceActivityService(DeviceActivityService deviceActivityService) {

        this.deviceActivityService = deviceActivityService;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void deviceSaveAndNotify(DeviceUpdate device, Set<Equipment> equipmentSet,
                                    HivePrincipal principal, boolean useExistingEquipment) {
        checkDevice(device);
        DeviceNotification dn;
        if (principal != null && principal.isAuthenticated()) {
            switch (principal.getRole()) {
                case HiveRoles.ADMIN:
                    dn = self.deviceSaveByUser(device, equipmentSet, principal.getUser(), useExistingEquipment);
                    break;
                case HiveRoles.CLIENT:
                    dn = self.deviceSaveByUser(device, equipmentSet, principal.getUser(), useExistingEquipment);
                    break;
                case HiveRoles.DEVICE:
                    dn = self.deviceUpdateByDevice(device, equipmentSet, principal.getDevice(), useExistingEquipment);
                    break;
                case HiveRoles.KEY:
                    dn = self.deviceSaveByKey(device, equipmentSet, principal.getKey(), useExistingEquipment);
                    break;
                default:
                    dn = self.deviceSave(device, equipmentSet, useExistingEquipment);
                    break;
            }
        } else {
            dn = self.deviceSave(device, equipmentSet, useExistingEquipment);
        }
        globalMessageBus.publishDeviceNotification(dn);
        deviceActivityService.update(dn.getDevice().getId());
    }

    public DeviceNotification deviceSaveByUser(DeviceUpdate deviceUpdate,
                                               Set<Equipment> equipmentSet,
                                               User user,
                                               boolean useExistingEquipment) {
        Network network = networkService.createOrUpdateNetworkByUser(deviceUpdate.getNetwork(), user);
        DeviceClass deviceClass = deviceClassService
                .createOrUpdateDeviceClass(deviceUpdate.getDeviceClass(), equipmentSet, useExistingEquipment);
        Device existingDevice = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceUpdate.getGuid().getValue());
        if (existingDevice == null) {
            Device device = deviceUpdate.convertTo();
            if (deviceClass != null) {
                device.setDeviceClass(deviceClass);
            }
            if (network != null) {
                device.setNetwork(network);
            }
            existingDevice = deviceDAO.createDevice(device);
            final DeviceNotification addDeviceNotification = ServerResponsesFactory.createNotificationForDevice
                    (existingDevice, SpecialNotifications.DEVICE_ADD);
            List<DeviceNotification> resultList =
                    deviceNotificationService.saveDeviceNotification(Arrays.asList(addDeviceNotification));
            return resultList.get(0);
        } else {
            if (!userService.hasAccessToDevice(user, existingDevice)) {
                throw new HiveException("Not authorized!", UNAUTHORIZED.getStatusCode());
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
            final DeviceNotification addDeviceNotification = ServerResponsesFactory.createNotificationForDevice
                    (existingDevice,
                            SpecialNotifications.DEVICE_UPDATE);
            List<DeviceNotification> resultList =
                    deviceNotificationService.saveDeviceNotification(Arrays.asList(addDeviceNotification));
            return resultList.get(0);
        }
    }

    public DeviceNotification deviceSaveByKey(DeviceUpdate deviceUpdate,
                                              Set<Equipment> equipmentSet,
                                              AccessKey key,
                                              boolean useExistingEquipment) {

        Device existingDevice = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceUpdate.getGuid().getValue());
        if (existingDevice != null && !accessKeyService.hasAccessToNetwork(key, existingDevice.getNetwork())) {
            throw new HiveException("No access to device with such guid", UNAUTHORIZED.getStatusCode());
        }
        Network network = networkService.verifyNetworkByKey(deviceUpdate.getNetwork(), key);
        DeviceClass deviceClass = deviceClassService
                .createOrUpdateDeviceClass(deviceUpdate.getDeviceClass(), equipmentSet, useExistingEquipment);
        if (existingDevice == null) {
            Device device = deviceUpdate.convertTo();       //is it allowed to create new device for specified guid
            // set?
            device.setDeviceClass(deviceClass);
            existingDevice = deviceDAO.createDevice(device);
            final DeviceNotification addDeviceNotification = ServerResponsesFactory.createNotificationForDevice
                    (existingDevice, SpecialNotifications.DEVICE_ADD);
            List<DeviceNotification> resultList =
                    deviceNotificationService.saveDeviceNotification(Arrays.asList(addDeviceNotification));
            return resultList.get(0);
        } else {
            if (!accessKeyService.hasAccessToDevice(key, deviceUpdate.getGuid().getValue())) {
                throw new HiveException("No access to device with such guid", UNAUTHORIZED.getStatusCode());
            }
            if (deviceUpdate.getDeviceClass() != null && !existingDevice.getDeviceClass().getPermanent()) {
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
            final DeviceNotification addDeviceNotification = ServerResponsesFactory.createNotificationForDevice
                    (existingDevice, SpecialNotifications.DEVICE_UPDATE);
            List<DeviceNotification> resultList =
                    deviceNotificationService.saveDeviceNotification(Arrays.asList(addDeviceNotification));
            return resultList.get(0);
        }
    }

    public DeviceNotification deviceUpdateByDevice(DeviceUpdate deviceUpdate,
                                                   Set<Equipment> equipmentSet,
                                                   Device device,
                                                   boolean useExistingEquipment) {
        if (deviceUpdate.getGuid() == null || !device.getGuid().equals(deviceUpdate.getGuid().getValue())) {
            throw new HiveException("No permissions to update another device!", UNAUTHORIZED.getStatusCode());
        }
        if (deviceUpdate.getKey() != null && !device.getKey().equals(deviceUpdate.getKey().getValue())) {
            throw new HiveException("Unauthorized! Wrong key or guid!", UNAUTHORIZED.getStatusCode());
        }
        DeviceClass deviceClass = deviceClassService
                .createOrUpdateDeviceClass(deviceUpdate.getDeviceClass(), equipmentSet, useExistingEquipment);
        Device existingDevice = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceUpdate.getGuid().getValue());
        if (deviceUpdate.getDeviceClass() != null && !existingDevice.getDeviceClass().getPermanent()) {
            existingDevice.setDeviceClass(deviceClass);
        }
        if (deviceUpdate.getStatus() != null) {
            existingDevice.setStatus(deviceUpdate.getStatus().getValue());
        }
        if (deviceUpdate.getData() != null) {
            existingDevice.setData(deviceUpdate.getData().getValue());
        }
        if (deviceUpdate.getName() != null) {
            existingDevice.setName(deviceUpdate.getName().getValue());
        }
        if (deviceUpdate.getKey() != null) {
            existingDevice.setKey(deviceUpdate.getKey().getValue());
        }
        final DeviceNotification addDeviceNotification = ServerResponsesFactory.createNotificationForDevice
                (existingDevice,
                        SpecialNotifications.DEVICE_UPDATE);
        List<DeviceNotification> resultList =
                deviceNotificationService.saveDeviceNotification(Arrays.asList(addDeviceNotification));
        return resultList.get(0);
    }

    public DeviceNotification deviceSave(DeviceUpdate deviceUpdate,
                                         Set<Equipment> equipmentSet,
                                         boolean useExistingEquipment) {
        Network network = networkService.createOrVeriryNetwork(deviceUpdate.getNetwork());
        DeviceClass deviceClass = deviceClassService
                .createOrUpdateDeviceClass(deviceUpdate.getDeviceClass(), equipmentSet, useExistingEquipment);
        Device existingDevice = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceUpdate.getGuid().getValue());

        if (existingDevice == null) {
            Device device = deviceUpdate.convertTo();
            if (deviceClass != null) {
                device.setDeviceClass(deviceClass);
            }
            if (network != null) {
                device.setNetwork(network);
            }
            existingDevice = deviceDAO.createDevice(device);
            final DeviceNotification addDeviceNotification = ServerResponsesFactory.createNotificationForDevice
                    (existingDevice,
                            SpecialNotifications.DEVICE_ADD);
            List<DeviceNotification> resultList =
                    deviceNotificationService.saveDeviceNotification(Arrays.asList(addDeviceNotification));
            return resultList.get(0);
        } else {
            if (deviceUpdate.getKey() == null || !existingDevice.getKey().equals(deviceUpdate.getKey().getValue())) {
                throw new HiveException("Unauthorized! Wrong key or guid!", UNAUTHORIZED.getStatusCode());
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
            final DeviceNotification addDeviceNotification = ServerResponsesFactory.createNotificationForDevice
                    (existingDevice,
                            SpecialNotifications.DEVICE_UPDATE);
            List<DeviceNotification> resultList =
                    deviceNotificationService.saveDeviceNotification(Arrays.asList(addDeviceNotification));
            return resultList.get(0);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUID(String uuid, User user, Set<AccessKeyPermission> permissions) {
        List<Device> result = deviceDAO.getDeviceList(user, permissions, Arrays.asList(uuid));
        return result.isEmpty() ? null : result.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Device> findByUUIDListAndUser(User user, Set<AccessKeyPermission> permissions, List<String> list) {
         return deviceDAO.getDeviceList(user, permissions, list);
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
    public void checkDevice(DeviceUpdate device) throws HiveException {
        if (device == null) {
            throw new HiveException("Device is empty");
        }
        if (device.getName() != null && device.getName() == null) {
            throw new HiveException("Device name is empty");
        }
        if (device.getKey() != null && device.getKey() == null) {
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
    public Device getDevice(String deviceId, User currentUser, Set<AccessKeyPermission> permissions) {
        List<Device> found = deviceDAO.getDeviceList(currentUser, permissions, Arrays.asList(deviceId));
        if (found.isEmpty()) {
            throw new HiveException("Device Not found", NOT_FOUND.getStatusCode());
        }
        return found.get(0);
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

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Set<Device> getDevicesForAccessKey(@NotNull AccessKey key, @NotNull Network network) {
        Set<AccessKeyPermission> permissions = key.getPermissions();
        Set<String> allGuids = new HashSet<>();
        for (AccessKeyPermission currentPermission : permissions) {
            if (currentPermission.getDeviceGuidsAsSet() == null) {
                allGuids.add(null);
                break;
            } else {
                allGuids.addAll(currentPermission.getDeviceGuidsAsSet());
            }
        }
        Set<Device> result = new HashSet<>();
        if (allGuids.contains(null)) {
            result.addAll(deviceDAO.findByNetwork(network));
        } else {
            result.addAll(deviceDAO.findByUUIDListAndNetwork(allGuids, network));
        }
        return result;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device authenticate(String uuid, String key) {
        Device device = deviceDAO.findByUUIDAndKey(uuid, key);
        if (device != null) {
            deviceActivityService.update(device.getId());
        }
        return device;
    }

    public boolean deleteDevice(@NotNull String guid, @NotNull User user) {
        List<Device> existing = deviceDAO.getDeviceList(user, null, Arrays.asList(guid));
        return existing.isEmpty() || deviceDAO.deleteDevice(guid);
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
                                User user,
                                Collection<AccessKeyBasedFilter> extraFilters) {

        return deviceDAO.getList(name, namePattern, status, networkId, networkName, deviceClassId, deviceClassName,
                deviceClassVersion, sortField, sortOrderAsc, take, skip, user, extraFilters);
    }

    public long getAllowedDevicesCount(User user, Set<AccessKeyPermission> permissions, List<String> guids){
        return deviceDAO.getNumberOfAvailableDevices(user, permissions, guids);
    }

}
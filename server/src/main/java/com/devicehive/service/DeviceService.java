package com.devicehive.service;

import com.devicehive.auth.CheckPermissionsHelper;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.util.HiveValidator;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ServerResponsesFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.util.*;

import static javax.ws.rs.core.Response.Status.*;

@Stateless
@LogExecutionTime
@EJB(beanInterface = DeviceService.class, name = "DeviceService")
public class DeviceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceService.class);

    @EJB
    private DeviceNotificationService deviceNotificationService;
    @EJB
    private DeviceDAO deviceDAO;
    @EJB
    private NetworkService networkService;
    @EJB
    private UserService userService;
    @EJB
    private DeviceClassService deviceClassService;
    @EJB
    private DeviceActivityService deviceActivityService;
    @EJB
    private AccessKeyService accessKeyService;
    @EJB
    private DeviceCommandService deviceCommandService;
    @EJB
    private HiveValidator hiveValidator;

    public void deviceSaveAndNotify(DeviceUpdate device, Set<Equipment> equipmentSet,
                                    HivePrincipal principal) {
        validateDevice(device);
        DeviceNotification dn;
        if (principal != null && principal.isAuthenticated()) {
            switch (principal.getRole()) {
                case HiveRoles.ADMIN:
                    dn = deviceSaveByUser(device, equipmentSet, principal.getUser());
                    break;
                case HiveRoles.CLIENT:
                    dn = deviceSaveByUser(device, equipmentSet, principal.getUser());
                    break;
                case HiveRoles.DEVICE:
                    dn = deviceUpdateByDevice(device, equipmentSet, principal.getDevice());
                    break;
                case HiveRoles.KEY:
                    dn = deviceSaveByKey(device, equipmentSet, principal.getKey());
                    break;
                default:
                    dn = deviceSave(device, equipmentSet);
                    break;
            }
        } else {
            dn = deviceSave(device, equipmentSet);
        }
        deviceNotificationService.submitDeviceNotification(dn, device.getGuid().getValue());
        deviceActivityService.update(device.getGuid().getValue());
    }

    public DeviceNotification deviceSaveByUser(DeviceUpdate deviceUpdate,
                                               Set<Equipment> equipmentSet,
                                               User user) {
        Network network = networkService.createOrUpdateNetworkByUser(deviceUpdate.getNetwork(), user);
        DeviceClass deviceClass = deviceClassService
            .createOrUpdateDeviceClass(deviceUpdate.getDeviceClass(), equipmentSet);
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
            deviceNotificationService.submitDeviceNotification(addDeviceNotification, existingDevice.getGuid());
            return addDeviceNotification;
        } else {
            if (!userService.hasAccessToDevice(user, existingDevice.getGuid())) {
                LOGGER.error("User {} has no access to device {}", user.getId(), existingDevice.getGuid());
                throw new HiveException(Messages.UNAUTHORIZED_REASON_PHRASE, UNAUTHORIZED.getStatusCode());
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
            deviceNotificationService.submitDeviceNotification(addDeviceNotification, existingDevice.getGuid());
            return addDeviceNotification;
        }
    }

    public DeviceNotification deviceSaveByKey(DeviceUpdate deviceUpdate,
                                              Set<Equipment> equipmentSet,
                                              AccessKey key) {

        Device existingDevice = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceUpdate.getGuid().getValue());
        if (existingDevice != null && !accessKeyService.hasAccessToNetwork(key, existingDevice.getNetwork())) {
            LOGGER.error("Access key {} has no access to device network {}", key, existingDevice.getGuid());
            throw new HiveException(
                String.format(Messages.DEVICE_NOT_FOUND, deviceUpdate.getGuid().getValue()), UNAUTHORIZED.getStatusCode());
        }
        Network network = networkService.verifyNetworkByKey(deviceUpdate.getNetwork(), key);
        DeviceClass deviceClass = deviceClassService
            .createOrUpdateDeviceClass(deviceUpdate.getDeviceClass(), equipmentSet);
        if (existingDevice == null) {
            Device device = deviceUpdate.convertTo();
            device.setDeviceClass(deviceClass);
            existingDevice = deviceDAO.createDevice(device);
            final DeviceNotification addDeviceNotification = ServerResponsesFactory.createNotificationForDevice
                (existingDevice, SpecialNotifications.DEVICE_ADD);
            deviceNotificationService.submitDeviceNotification(addDeviceNotification, existingDevice.getGuid());
            return addDeviceNotification;
        } else {
            if (!accessKeyService.hasAccessToDevice(key, deviceUpdate.getGuid().getValue())) {
                LOGGER.error("Access key {} has no access to device network {}", key, existingDevice.getGuid());
                throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceUpdate.getGuid().getValue()),
                    UNAUTHORIZED.getStatusCode());
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
            deviceNotificationService.submitDeviceNotification(addDeviceNotification, existingDevice.getGuid());
            return addDeviceNotification;
        }
    }

    public DeviceNotification deviceUpdateByDevice(DeviceUpdate deviceUpdate,
                                                   Set<Equipment> equipmentSet,
                                                   Device device) {
        if (deviceUpdate.getGuid() == null) {
            LOGGER.error("Device guid not found in deviceUpdate request");
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, BAD_REQUEST.getStatusCode());
        }
        if (!device.getGuid().equals(deviceUpdate.getGuid().getValue())) {
            LOGGER.error("Device update guid {} doesn't equal to the authenticated device guid {}",
                    deviceUpdate.getGuid().getValue(), device.getGuid());
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceUpdate.getGuid().getValue()),
                                    UNAUTHORIZED.getStatusCode());
        }
        if (deviceUpdate.getKey() != null && !device.getKey().equals(deviceUpdate.getKey().getValue())) {
            LOGGER.error("Device update key {} doesn't equal to the authenticated device key {}",
                    deviceUpdate.getKey().getValue(), device.getKey());
            throw new HiveException(Messages.INCORRECT_CREDENTIALS, UNAUTHORIZED.getStatusCode());
        }
        DeviceClass deviceClass = deviceClassService
            .createOrUpdateDeviceClass(deviceUpdate.getDeviceClass(), equipmentSet);
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
        deviceNotificationService.submitDeviceNotification(addDeviceNotification, existingDevice.getGuid());
        return addDeviceNotification;
    }

    public DeviceNotification deviceSave(DeviceUpdate deviceUpdate,
                                         Set<Equipment> equipmentSet) {
        Network network = networkService.createOrVeriryNetwork(deviceUpdate.getNetwork());
        DeviceClass deviceClass = deviceClassService
            .createOrUpdateDeviceClass(deviceUpdate.getDeviceClass(), equipmentSet);
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
            deviceNotificationService.submitDeviceNotification(addDeviceNotification, existingDevice.getGuid());
            return addDeviceNotification;
        } else {
            if (deviceUpdate.getKey() == null || !existingDevice.getKey().equals(deviceUpdate.getKey().getValue())) {
                LOGGER.error("Device update key {} doesn't equal to the authenticated device key {}",
                        deviceUpdate.getKey().getValue(), existingDevice.getKey());
                throw new HiveException(Messages.INCORRECT_CREDENTIALS, UNAUTHORIZED.getStatusCode());
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
            deviceNotificationService.submitDeviceNotification(addDeviceNotification, existingDevice.getGuid());
            return addDeviceNotification;
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByGuidWithPermissionsCheck(String guid, HivePrincipal principal) {
        List<Device> result = findByGuidWithPermissionsCheck(Arrays.asList(guid), principal);
        return result.isEmpty() ? null : result.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Device> findByGuidWithPermissionsCheck(Collection<String> guids, HivePrincipal principal) {
        return deviceDAO.getDeviceList(principal, guids);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @SuppressWarnings("unchecked")
    public List<String> findGuidsWithPermissionsCheck(Collection<String> guids, HivePrincipal principal) {
        final List<Device> devices =  deviceDAO.getDeviceList(principal, guids);
        return (List<String>) CollectionUtils.collect(devices, new Transformer() {
            @Override
            public Object transform(Object o) {
                return ((Device) o).getGuid();
            }
        });
    }

    /**
     * Implementation for model: if field exists and null - error if field does not exists - use field from database
     *
     * @param device device to check
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void validateDevice(DeviceUpdate device) throws HiveException {
        if (device == null) {
            LOGGER.error("Device validation: device is empty");
            throw new HiveException(Messages.EMPTY_DEVICE);
        }
        if (device.getName() != null && device.getName().getValue() == null) {
            LOGGER.error("Device validation: device name is empty");
            throw new HiveException(Messages.EMPTY_DEVICE_NAME);
        }
        if (device.getKey() != null && device.getKey().getValue() == null) {
            LOGGER.error("Device validation: device key is empty");
            throw new HiveException(Messages.EMPTY_DEVICE_KEY);
        }
        if (device.getDeviceClass() != null && device.getDeviceClass().getValue() == null) {
            LOGGER.error("Device validation: device class is empty");
            throw new HiveException(Messages.EMPTY_DEVICE_CLASS);
        }
        hiveValidator.validate(device);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Device getDeviceWithNetworkAndDeviceClass(String deviceId, HivePrincipal principal) {

        if (getAllowedDevicesCount(principal, Arrays.asList(deviceId)) == 0) {
            LOGGER.error("Allowed device count is equal to 0");
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), NOT_FOUND.getStatusCode());
        }

        Device device = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceId);

        if (device == null) {
            LOGGER.error("Device with guid {} not found", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), NOT_FOUND.getStatusCode());
        }
        return device;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Device authenticate(String uuid, String key) {
        Device device = deviceDAO.findByUUIDAndKey(uuid, key);
        if (device != null) {
            deviceActivityService.update(device.getGuid());
        }
        return device;
    }

    public boolean deleteDevice(@NotNull String guid, HivePrincipal principal) {
        List<Device> existing = deviceDAO.getDeviceList(principal, Arrays.asList(guid));
        return existing.isEmpty() || deviceDAO.deleteDevice(guid);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
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
                                HivePrincipal principal) {

        return deviceDAO.getList(name, namePattern, status, networkId, networkName, deviceClassId, deviceClassName,
                                 deviceClassVersion, sortField, sortOrderAsc, take, skip, principal);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Device> getList(Long networkId,
                                HivePrincipal principal) {
        return deviceDAO
            .getList(null, null, null, networkId, null, null, null, null, null, null, null, null, principal);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long getAllowedDevicesCount(HivePrincipal principal, List<String> guids) {
        return deviceDAO.getNumberOfAvailableDevices(principal, guids);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<Device, Set<String>> createFilterMap(@NotNull Map<String, Set<String>> requested,
                                                    HivePrincipal principal) {

        List<Device> allowedDevices = findByGuidWithPermissionsCheck(requested.keySet(), principal);
        Map<String, Device> uuidToDevice = new HashMap<>();
        for (Device device : allowedDevices) {
            uuidToDevice.put(device.getGuid(), device);
        }

        Set<String> noAccessUuid = new HashSet<>();

        Map<Device, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : requested.entrySet()) {
            String uuid = entry.getKey();
            if (uuidToDevice.containsKey(uuid)) {
                result.put(uuidToDevice.get(uuid), entry.getValue());
            } else {
                noAccessUuid.add(uuid);
            }
        }
        if (!noAccessUuid.isEmpty()) {
            String message = String.format(Messages.DEVICES_NOT_FOUND, StringUtils.join(noAccessUuid, ","));
            throw new HiveException(message, Response.Status.NOT_FOUND.getStatusCode());
        }
        return result;
    }


    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean hasAccessTo(@NotNull HivePrincipal filtered, @NotNull String deviceGuid) {
        if (filtered.getDevice() != null) {
            return filtered.getDevice().getGuid().equals(deviceGuid);
        }
        if (filtered.getUser() != null) {
            return userService.hasAccessToDevice(filtered.getUser(), deviceGuid);
        }
        if (filtered.getKey() != null) {
            if (!userService.hasAccessToDevice(filtered.getKey().getUser(), deviceGuid)) {
                return false;
            }
            return CheckPermissionsHelper.checkFilteredPermissions(filtered.getKey().getPermissions(),
                    deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceGuid));
        }
        return false;
    }

}
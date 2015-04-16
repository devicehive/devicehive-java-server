package com.devicehive.service;

import com.devicehive.auth.CheckPermissionsHelper;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.bus.Create;
import com.devicehive.messages.bus.GlobalMessage;
import com.devicehive.messages.bus.LocalMessage;
import com.devicehive.model.AccessKey;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.Equipment;
import com.devicehive.model.Network;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.User;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.util.HiveValidator;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ServerResponsesFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.util.*;

import static javax.ws.rs.core.Response.Status.*;

@Stateless
@LogExecutionTime
@EJB(beanInterface = DeviceService.class, name = "DeviceService")
public class DeviceService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
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
    private HiveValidator hiveValidator;
    @Inject
    @Create
    @LocalMessage
    private Event<DeviceNotification> eventLocal;

    @Inject
    @Create
    @GlobalMessage
    private Event<DeviceNotification> eventGlobal;

    public void deviceSaveAndNotify(DeviceUpdate device, Set<Equipment> equipmentSet,
                                    HivePrincipal principal) {
        logger.debug("Device: {}. Current role: {}.", device.getGuid(), principal == null ? null : principal.getRole());
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
        eventLocal.fire(dn);
        eventGlobal.fire(dn);
        deviceActivityService.update(dn.getDevice().getId());
    }

    public DeviceNotification deviceSaveByUser(DeviceUpdate deviceUpdate,
                                               Set<Equipment> equipmentSet,
                                               User user) {
        logger.debug("Device save executed for device: id {}, user: {}", deviceUpdate.getGuid(), user.getId());
        Network network = networkService.createOrUpdateNetworkByUser(deviceUpdate.getNetwork(), user);
        Device existingDevice = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceUpdate.getGuid().getValue());
        boolean isExistingDevice = (existingDevice != null);
        DeviceClass deviceClass = deviceClassService
            .createOrUpdateDeviceClass(deviceUpdate.getDeviceClass(), equipmentSet, isExistingDevice);

        if (isExistingDevice) {
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
            List<DeviceNotification> resultList =
                deviceNotificationService.saveDeviceNotification(Arrays.asList(addDeviceNotification));
            return resultList.get(0);
        }
    }

    public DeviceNotification deviceSaveByKey(DeviceUpdate deviceUpdate,
                                              Set<Equipment> equipmentSet,
                                              AccessKey key) {
        logger.debug("Device save executed for device: id {}, user: {}", deviceUpdate.getGuid(), key.getKey());
        Device existingDevice = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceUpdate.getGuid().getValue());
        boolean isExistingDevice = (existingDevice != null);
        if (isExistingDevice && !accessKeyService.hasAccessToNetwork(key, existingDevice.getNetwork())) {
            throw new HiveException(
                String.format(Messages.DEVICE_NOT_FOUND, deviceUpdate.getGuid().getValue()),
                UNAUTHORIZED.getStatusCode());
        }
        Network network = networkService.createOrVeriryNetworkByKey(deviceUpdate.getNetwork(), key);
        DeviceClass deviceClass = deviceClassService
            .createOrUpdateDeviceClass(deviceUpdate.getDeviceClass(), equipmentSet, isExistingDevice);
        if (isExistingDevice) {
            Device device = deviceUpdate.convertTo();
            device.setDeviceClass(deviceClass);
            device.setNetwork(network);
            existingDevice = deviceDAO.createDevice(device);
            final DeviceNotification addDeviceNotification = ServerResponsesFactory.createNotificationForDevice
                (existingDevice, SpecialNotifications.DEVICE_ADD);
            List<DeviceNotification> resultList =
                deviceNotificationService.saveDeviceNotification(Arrays.asList(addDeviceNotification));
            return resultList.get(0);
        } else {
            if (!accessKeyService.hasAccessToDevice(key, deviceUpdate.getGuid().getValue())) {
                throw new HiveException(
                    String.format(Messages.DEVICE_NOT_FOUND, deviceUpdate.getGuid().getValue()),
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
            List<DeviceNotification> resultList =
                deviceNotificationService.saveDeviceNotification(Arrays.asList(addDeviceNotification));
            return resultList.get(0);
        }
    }

    public DeviceNotification deviceUpdateByDevice(DeviceUpdate deviceUpdate,
                                                   Set<Equipment> equipmentSet,
                                                   Device device) {
        logger.debug("Device update executed for device update: id {}, device: {}", deviceUpdate.getGuid(),
                     device.getId());
        if (deviceUpdate.getGuid() == null) {
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, BAD_REQUEST.getStatusCode());
        }
        if (!device.getGuid().equals(deviceUpdate.getGuid().getValue())) {
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceUpdate.getGuid().getValue()),
                                    UNAUTHORIZED.getStatusCode());
        }
        if (deviceUpdate.getKey() != null && !device.getKey().equals(deviceUpdate.getKey().getValue())) {
            throw new HiveException(Messages.INCORRECT_CREDENTIALS, UNAUTHORIZED.getStatusCode());
        }
        Device existingDevice = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceUpdate.getGuid().getValue());
        boolean isExistingDevice = (existingDevice != null);
        DeviceClass deviceClass = deviceClassService
                .createOrUpdateDeviceClass(deviceUpdate.getDeviceClass(), equipmentSet, isExistingDevice);
        if (deviceUpdate.getDeviceClass() != null && !existingDevice.getDeviceClass().getPermanent()) {
            existingDevice.setDeviceClass(deviceClass);
        }
        if (deviceUpdate.getNetwork() != null) {
            Network network = networkService.createOrVeriryNetwork(deviceUpdate.getNetwork());
            existingDevice.setNetwork(network);
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
                                         Set<Equipment> equipmentSet) {
        logger.debug("Device save executed for device update: id {}", deviceUpdate.getGuid());
        Network network = networkService.createOrVeriryNetwork(deviceUpdate.getNetwork());
        Device existingDevice = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceUpdate.getGuid().getValue());
        boolean isExistingDevice = (existingDevice != null);
        DeviceClass deviceClass = deviceClassService
            .createOrUpdateDeviceClass(deviceUpdate.getDeviceClass(), equipmentSet, isExistingDevice);

        if (isExistingDevice) {
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
            List<DeviceNotification> resultList =
                deviceNotificationService.saveDeviceNotification(Arrays.asList(addDeviceNotification));
            return resultList.get(0);
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

    /**
     * Implementation for model: if field exists and null - error if field does not exists - use field from database
     *
     * @param device device to check
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void validateDevice(DeviceUpdate device) throws HiveException {
        if (device == null) {
            throw new HiveException(Messages.EMPTY_DEVICE);
        }
        if (device.getName() != null && device.getName() == null) {
            throw new HiveException(Messages.EMPTY_DEVICE_NAME);
        }
        if (device.getKey() != null && device.getKey() == null) {
            throw new HiveException(Messages.EMPTY_DEVICE_KEY);
        }
        if (device.getDeviceClass() != null && device.getDeviceClass().getValue() == null) {
            throw new HiveException(Messages.EMPTY_DEVICE_CLASS);
        }
        hiveValidator.validate(device);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Device getDeviceWithNetworkAndDeviceClass(String deviceId, HivePrincipal principal) {

        if (getAllowedDevicesCount(principal, Arrays.asList(deviceId)) == 0) {
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), NOT_FOUND.getStatusCode());
        }

        Device device = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceId);

        if (device == null) {
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), NOT_FOUND.getStatusCode());
        }
        return device;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Device authenticate(String uuid, String key) {
        Device device = deviceDAO.findByUUIDAndKey(uuid, key);
        if (device != null) {
            deviceActivityService.update(device.getId());
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
    public boolean hasAccessTo(@NotNull HivePrincipal filtered, @NotNull Device device) {
        if (filtered.getDevice() != null) {
            return filtered.getDevice().getId().equals(device.getId());
        }
        if (filtered.getUser() != null) {
            return userService.hasAccessToDevice(filtered.getUser(), device);
        }
        if (filtered.getKey() != null) {
            if (!userService.hasAccessToDevice(filtered.getKey().getUser(), device)) {
                return false;
            }
            return CheckPermissionsHelper.checkFilteredPermissions(filtered.getKey().getPermissions(), device);
        }
        return false;
    }

}
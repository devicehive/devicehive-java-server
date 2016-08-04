package com.devicehive.service;

import com.devicehive.auth.CheckPermissionsHelper;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceDao;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.util.HiveValidator;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.*;

@Component
public class DeviceService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    @Autowired
    private DeviceNotificationService deviceNotificationService;
    @Autowired
    private NetworkService networkService;
    @Autowired
    private UserService userService;
    @Autowired
    private DeviceClassService deviceClassService;
    @Autowired
    private DeviceActivityService deviceActivityService;
    @Autowired
    private AccessKeyService accessKeyService;
    @Autowired
    private HiveValidator hiveValidator;
    @Autowired
    private DeviceDao deviceDao;

    @Transactional(propagation = Propagation.REQUIRED)
    public void deviceSaveAndNotify(DeviceUpdate device, Set<DeviceClassEquipmentVO> equipmentSet, HivePrincipal principal) {
        logger.debug("Device: {}. Current role: {}.", device.getGuid(), principal == null ? null : principal.getRole());
        validateDevice(device);
        DeviceNotification dn;
        if (principal != null && principal.isAuthenticated()) {
            switch (principal.getRole()) {
                case HiveRoles.ADMIN:
                case HiveRoles.CLIENT:
                    dn = deviceSaveByUser(device, equipmentSet, principal.getUser());
                    break;
                case HiveRoles.KEY:
                    dn = deviceSaveByKey(device, equipmentSet, principal.getKey());
                    break;
                default:
                    throw new HiveException(Messages.INVALID_USER_ROLE, FORBIDDEN.getStatusCode());
            }
        } else {
            throw new HiveException(Messages.UNAUTHORIZED_REASON_PHRASE, UNAUTHORIZED.getStatusCode());
        }
        deviceNotificationService.submitDeviceNotification(dn, device.getGuid().orElse(null));
        deviceActivityService.update(device.getGuid().orElse(null));
    }

    //TODO [rafa] equipmentSet parameter looks strange
    private DeviceNotification deviceSaveByUser(DeviceUpdate deviceUpdate,
                                               Set<DeviceClassEquipmentVO> equipmentSet,
                                               User user) {
        logger.debug("Device save executed for device: id {}, user: {}", deviceUpdate.getGuid(), user.getId());
        //todo: rework when migration to VO will be done
        NetworkVO vo = deviceUpdate.getNetwork() != null ? deviceUpdate.getNetwork().get() : null;
        NetworkVO nwVo = networkService.createOrUpdateNetworkByUser(Optional.ofNullable(vo), user);
        NetworkVO network = nwVo != null ? findNetworkForAuth(nwVo) : null;
        network = findNetworkForAuth(network);
        DeviceClassWithEquipmentVO deviceClass = prepareDeviceClassForNewlyCreatedDevice(deviceUpdate);
        // TODO [requies a lot of details]!
        DeviceVO existingDevice = deviceDao.findByUUID(deviceUpdate.getGuid().orElse(null));
        if (existingDevice == null) {
            DeviceVO device = deviceUpdate.convertTo();
            if (deviceClass != null) {
                //TODO [rafa] changed
                DeviceClassVO dc = new DeviceClassVO();
                dc.setId(deviceClass.getId());
                dc.setName(deviceClass.getName());
                dc.setIsPermanent(deviceClass.getIsPermanent());
                device.setDeviceClass(dc);
            }
            if (network != null) {
                device.setNetwork(network);
            }
            if (device.getBlocked() == null) {
                device.setBlocked(false);
            }
            deviceDao.persist(device);
            return ServerResponsesFactory.createNotificationForDevice(device, SpecialNotifications.DEVICE_ADD);
        } else {
            if (!userService.hasAccessToDevice(user, existingDevice.getGuid())) {
                logger.error("User {} has no access to device {}", user.getId(), existingDevice.getGuid());
                throw new HiveException(Messages.NO_ACCESS_TO_DEVICE, FORBIDDEN.getStatusCode());
            }
            if (deviceUpdate.getDeviceClass() != null) {
                DeviceClassVO dc = new DeviceClassVO();
                dc.setId(deviceClass.getId());
                dc.setName(deviceClass.getName());
                dc.setIsPermanent(deviceClass.getIsPermanent());
                existingDevice.setDeviceClass(dc);
            }
            if (deviceUpdate.getStatus() != null) {
                existingDevice.setStatus(deviceUpdate.getStatus().orElse(null));
            }
            if (deviceUpdate.getData() != null) {
                existingDevice.setData(deviceUpdate.getData().orElse(null));
            }
            if (deviceUpdate.getNetwork() != null) {
                existingDevice.setNetwork(network);
            }
            if (deviceUpdate.getName() != null) {
                existingDevice.setName(deviceUpdate.getName().orElse(null));
            }
            if (deviceUpdate.getBlocked() != null) {
                existingDevice.setBlocked(deviceUpdate.getBlocked().orElse(null));
            }
            deviceDao.merge(existingDevice);
            return ServerResponsesFactory.createNotificationForDevice(existingDevice, SpecialNotifications.DEVICE_UPDATE);
        }
    }

    private DeviceClassWithEquipmentVO prepareDeviceClassForNewlyCreatedDevice(DeviceUpdate deviceUpdate) {
        DeviceClassWithEquipmentVO deviceClass = null;
        if (deviceUpdate.getDeviceClass() != null && deviceUpdate.getDeviceClass().isPresent()) {
            //TODO [rafa] if device class equipment not present - we need to clone it from device.
            Set<DeviceClassEquipmentVO> customEquipmentSet = null;
            if (deviceUpdate.getDeviceClass().get().getEquipment() != null) {
                customEquipmentSet = deviceUpdate.getDeviceClass().get().getEquipment().orElse(new HashSet<>());
            }
            deviceClass = deviceClassService.createOrUpdateDeviceClass(deviceUpdate.getDeviceClass(), customEquipmentSet);
        }
        return deviceClass;
    }

    private DeviceNotification deviceSaveByKey(DeviceUpdate deviceUpdate,
                                              Set<DeviceClassEquipmentVO> equipmentSet,
                                              AccessKey key) {
        logger.debug("Device save executed for device: id {}, user: {}", deviceUpdate.getGuid(), key.getKey());
        //TODO [requires a lot of details]
        DeviceVO existingDevice = deviceDao.findByUUID(deviceUpdate.getGuid().orElse(null));
        if (existingDevice != null && !accessKeyService.hasAccessToNetwork(key, existingDevice.getNetwork())) {
            logger.error("Access key {} has no access to device network {}", key, existingDevice.getNetwork().getId());
            throw new HiveException(Messages.NO_ACCESS_TO_NETWORK, FORBIDDEN.getStatusCode());
        }
        NetworkVO nw = deviceUpdate.getNetwork() != null ? deviceUpdate.getNetwork().get() : null;
        NetworkVO network = networkService.createOrVerifyNetworkByKey(Optional.ofNullable(nw), key);
        network = findNetworkForAuth(network);
        DeviceClassWithEquipmentVO deviceClass = prepareDeviceClassForNewlyCreatedDevice(deviceUpdate);
        if (existingDevice == null) {
            DeviceClassVO dc = new DeviceClassVO();
            dc.setId(deviceClass.getId());
            dc.setName(deviceClass.getName());

            DeviceVO device = deviceUpdate.convertTo();
            device.setDeviceClass(dc);
            device.setNetwork(network);
            deviceDao.persist(device);
            return ServerResponsesFactory.createNotificationForDevice(device, SpecialNotifications.DEVICE_ADD);
        } else {
            if (!accessKeyService.hasAccessToDevice(key, deviceUpdate.getGuid().orElse(null))) {
                logger.error("Access key {} has no access to device {}", key, existingDevice.getGuid());
                throw new HiveException(Messages.NO_ACCESS_TO_DEVICE, FORBIDDEN.getStatusCode());
            }
            if (deviceUpdate.getDeviceClass() != null && !existingDevice.getDeviceClass().getIsPermanent()) {
                DeviceClassVO dc = new DeviceClassVO();
                dc.setId(deviceClass.getId());
                dc.setName(deviceClass.getName());
                existingDevice.setDeviceClass(dc);
            }
            if (deviceUpdate.getStatus() != null) {
                existingDevice.setStatus(deviceUpdate.getStatus().orElse(null));
            }
            if (deviceUpdate.getData() != null) {
                existingDevice.setData(deviceUpdate.getData().orElse(null));
            }
            if (deviceUpdate.getNetwork() != null) {
                existingDevice.setNetwork(network);
            }
            if (deviceUpdate.getName() != null) {
                existingDevice.setName(deviceUpdate.getName().orElse(null));
            }
            if (deviceUpdate.getBlocked() != null) {
                existingDevice.setBlocked(Boolean.TRUE.equals(deviceUpdate.getBlocked().orElse(null)));
            }
            deviceDao.merge(existingDevice);
            return ServerResponsesFactory.createNotificationForDevice(existingDevice, SpecialNotifications.DEVICE_UPDATE);
        }
    }

    @Transactional
    public DeviceNotification deviceSave(DeviceUpdate deviceUpdate,
                                         Set<DeviceClassEquipmentVO> equipmentSet) {
        logger.debug("Device save executed for device update: id {}", deviceUpdate.getGuid());
        NetworkVO network = (deviceUpdate.getNetwork() != null)? deviceUpdate.getNetwork().get() : null;
        if (network != null) {
            network = networkService.createOrVerifyNetwork(Optional.ofNullable(network));
        }
        DeviceClassWithEquipmentVO deviceClass = prepareDeviceClassForNewlyCreatedDevice(deviceUpdate);
        //TODO [requires a lot of details]
        DeviceVO existingDevice = deviceDao.findByUUID(deviceUpdate.getGuid().orElse(null));

        if (existingDevice == null) {
            DeviceVO device = deviceUpdate.convertTo();
            if (deviceClass != null) {
                DeviceClassVO dc = new DeviceClassVO();
                dc.setId(deviceClass.getId());
                dc.setName(deviceClass.getName());
                device.setDeviceClass(dc);
            }
            if (network != null) {
                device.setNetwork(network);
            }
            deviceDao.persist(device);
            return ServerResponsesFactory.createNotificationForDevice(device, SpecialNotifications.DEVICE_ADD);
        } else {
            if (deviceUpdate.getDeviceClass() != null) {
                DeviceClassVO dc = new DeviceClassVO();
                dc.setId(deviceClass.getId());
                dc.setName(deviceClass.getName());
                existingDevice.setDeviceClass(dc);
            }
            if (deviceUpdate.getStatus() != null) {
                existingDevice.setStatus(deviceUpdate.getStatus().orElse(null));
            }
            if (deviceUpdate.getData() != null) {
                existingDevice.setData(deviceUpdate.getData().orElse(null));
            }
            if (deviceUpdate.getNetwork() != null) {
                existingDevice.setNetwork(network);
            }
            if (deviceUpdate.getBlocked() != null) {
                existingDevice.setBlocked(Boolean.TRUE.equals(deviceUpdate.getBlocked().orElse(null)));
            }
            deviceDao.merge(existingDevice);
            return ServerResponsesFactory.createNotificationForDevice(existingDevice, SpecialNotifications.DEVICE_UPDATE);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceVO findByGuidWithPermissionsCheck(String guid, HivePrincipal principal) {
        List<DeviceVO> result = findByGuidWithPermissionsCheck(Arrays.asList(guid), principal);
        return result.isEmpty() ? null : result.get(0);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<DeviceVO> findByGuidWithPermissionsCheck(Collection<String> guids, HivePrincipal principal) {
        return getDeviceList(new ArrayList<>(guids), principal);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @SuppressWarnings("unchecked")
    public List<String> findGuidsWithPermissionsCheck(Collection<String> guids, HivePrincipal principal) {
        final List<DeviceVO> devices =  getDeviceList(new ArrayList<>(guids), principal);
        return devices.stream()
                .map(DeviceVO::getGuid)
                .collect(Collectors.toList());
    }

    /**
     * Implementation for model: if field exists and null - error if field does not exists - use field from database
     *
     * @param device device to check
     */
    @Transactional(readOnly = true)
    public void validateDevice(DeviceUpdate device) throws HiveException {
        if (device == null) {
            logger.error("Device validation: device is empty");
            throw new HiveException(Messages.EMPTY_DEVICE);
        }
        if (device.getName() != null && device.getName().orElse(null) == null) {
            logger.error("Device validation: device name is empty");
            throw new HiveException(Messages.EMPTY_DEVICE_NAME);
        }
        if (device.getDeviceClass() != null && device.getDeviceClass().orElse(null) == null) {
            logger.error("Device validation: device class is empty");
            throw new HiveException(Messages.EMPTY_DEVICE_CLASS);
        }
        hiveValidator.validate(device);
    }

    @Transactional(readOnly = true)
    public DeviceVO getDeviceWithNetworkAndDeviceClass(String deviceId, HivePrincipal principal) {

        if (getAllowedDevicesCount(principal, Arrays.asList(deviceId)) == 0) {
            logger.error("Allowed device count is equal to 0");
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), NOT_FOUND.getStatusCode());
        }

        //TODO with network and device class
        DeviceVO device = deviceDao.findByUUID(deviceId);

        if (device == null) {
            logger.error("Device with guid {} not found", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), NOT_FOUND.getStatusCode());
        }
        return device;
    }

    //TODO: only migrated to genericDAO, need to migrate Device PK to guid and use directly GenericDAO#remove
    @Transactional
    public boolean deleteDevice(@NotNull String guid, HivePrincipal principal) {
        List<DeviceVO> existing = getDeviceList(Arrays.asList(guid), principal);
        return existing.isEmpty() || deviceDao.deleteByUUID(guid) != 0;
    }

    @Transactional(readOnly = true)
    public List<DeviceVO> getList(String name,
                                String namePattern,
                                String status,
                                Long networkId,
                                String networkName,
                                Long deviceClassId,
                                String deviceClassName,
                                String sortField,
                                @NotNull Boolean sortOrderAsc,
                                Integer take,
                                Integer skip,
                                HivePrincipal principal) {
        return deviceDao.getList(name, namePattern, status, networkId, networkName, deviceClassId, deviceClassName,
                sortField, sortOrderAsc, take, skip, principal);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    //TODO: need to remove it
    public long getAllowedDevicesCount(HivePrincipal principal, List<String> guids) {
        return deviceDao.getAllowedDeviceCount(principal, guids);
    }

    @Transactional
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
                    deviceDao.findByUUID(deviceGuid));
        }
        return false;
    }

    private List<DeviceVO> getDeviceList(List<String> guids, HivePrincipal principal) {
        return deviceDao.getDeviceList(guids, principal);
    }


    private NetworkVO findNetworkForAuth(NetworkVO network) {
        if (network == null) {
            HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User user = findUserFromAuth(principal);
            if (user != null) {
                Set<Network> userNetworks = userService.findUserWithNetworks(user.getId()).getNetworks();
                if (userNetworks.isEmpty()) {
                    throw new HiveException(Messages.NO_NETWORKS_ASSIGNED_TO_USER, PRECONDITION_FAILED.getStatusCode());
                }

                Network firstNetwork = userNetworks.iterator().next();
                return Network.convertNetwork(firstNetwork);
            }
        }
        return network;
    }

    private User findUserFromAuth(HivePrincipal principal) {
        if (principal.getUser() != null) {
            return principal.getUser();
        }
        if (principal.getKey() != null && principal.getKey().getUser() != null)  {
            return principal.getKey().getUser();
        }
        return null;
    }

}

package com.devicehive.service;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceDao;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.rpc.ListDeviceRequest;
import com.devicehive.model.rpc.ListDeviceResponse;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
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
import java.util.concurrent.CompletableFuture;

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
    private TimestampService timestampService;
    @Autowired
    private HiveValidator hiveValidator;
    @Autowired
    private DeviceDao deviceDao;
    @Autowired
    private RpcClient rpcClient;

    //todo equipmentSet is not used
    @Transactional(propagation = Propagation.REQUIRED)
    public void deviceSaveAndNotify(DeviceUpdate device, Set<DeviceClassEquipmentVO> equipmentSet, HivePrincipal principal) {
        logger.debug("Device: {}. Current principal: {}.", device.getGuid(), principal == null ? null : principal.getName());
        validateDevice(device);
        DeviceNotification dn;
        if (principal != null && principal.isAuthenticated()) {
            if (principal.getUser() != null) {
                dn = deviceSaveByUser(device, principal.getUser());
            } else if (principal.getNetworkIds() != null && principal.getDeviceGuids() != null) {
                dn = deviceSaveByPrincipalPermissions(device, principal);
            } else {
                throw new HiveException(Messages.UNAUTHORIZED_REASON_PHRASE, UNAUTHORIZED.getStatusCode());
            }
        } else {
            throw new HiveException(Messages.UNAUTHORIZED_REASON_PHRASE, UNAUTHORIZED.getStatusCode());
        }
        dn.setTimestamp(timestampService.getDate());
        deviceNotificationService.insert(dn, device.convertTo());
        deviceActivityService.update(device.getGuid().orElse(null));
    }

    private DeviceNotification deviceSaveByUser(DeviceUpdate deviceUpdate, UserVO user) {
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

    private DeviceNotification deviceSaveByPrincipalPermissions(DeviceUpdate deviceUpdate, HivePrincipal principal) {
        logger.debug("Device save executed for device: id {}, user: {}", deviceUpdate.getGuid(), principal.getName());
        //TODO [requires a lot of details]
        DeviceVO existingDevice = deviceDao.findByUUID(deviceUpdate.getGuid().orElse(null));
        if (existingDevice != null && !principal.hasAccessToNetwork(existingDevice.getNetwork().getId())) {
            logger.error("Principal {} has no access to device network {}", principal.getName(), existingDevice.getNetwork().getId());
            throw new HiveException(Messages.NO_ACCESS_TO_NETWORK, FORBIDDEN.getStatusCode());
        }

        NetworkVO nw = deviceUpdate.getNetwork() != null ? deviceUpdate.getNetwork().get() : null;
        NetworkVO network = networkService.createOrVerifyNetwork(Optional.ofNullable(nw));
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
            if (!principal.hasAccessToDevice(deviceUpdate.getGuid().orElse(null))) {
                logger.error("Principal {} has no access to device {}", principal, existingDevice.getGuid());
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
        List<DeviceVO> result = findByGuidWithPermissionsCheck(Collections.singletonList(guid), principal);
        return result.isEmpty() ? null : result.get(0);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<DeviceVO> findByGuidWithPermissionsCheck(Collection<String> guids, HivePrincipal principal) {
        return getDeviceList(new ArrayList<>(guids), principal);
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
    public DeviceVO getDeviceWithNetworkAndDeviceClass(String deviceId) {
        DeviceVO device = deviceDao.findByUUID(deviceId);

        if (device == null) {
            logger.error("Device with guid {} not found", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), NOT_FOUND.getStatusCode());
        }
        return device;
    }

    //TODO: only migrated to genericDAO, need to migrate Device PK to guid and use directly GenericDAO#remove
    @Transactional
    public boolean deleteDevice(@NotNull String guid) {
        return deviceDao.deleteByUUID(guid) != 0;
    }

    //@Transactional(readOnly = true)
    public CompletableFuture<List<DeviceVO>> list(String name,
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
        ListDeviceRequest request = new ListDeviceRequest();
        request.setName(name);
        request.setNamePattern(namePattern);
        request.setStatus(status);
        request.setNetworkId(networkId);
        request.setNetworkName(networkName);
        request.setDeviceClassId(deviceClassId);
        request.setDeviceClassName(deviceClassName);
        request.setSortField(sortField);
        request.setSortOrderAsc(sortOrderAsc);
        request.setTake(take);
        request.setSkip(skip);
        request.setPrincipal(principal);

        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request.newBuilder().withBody(request).build(), new ResponseConsumer(future));
        return future.thenApply(r -> ((ListDeviceResponse) r.getBody()).getDevices());
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    //TODO: need to remove it
    public long getAllowedDevicesCount(HivePrincipal principal, List<String> guids) {
        return deviceDao.getAllowedDeviceCount(principal, guids);
    }

    private List<DeviceVO> getDeviceList(List<String> guids, HivePrincipal principal) {
        return deviceDao.getDeviceList(guids, principal);
    }


    private NetworkVO findNetworkForAuth(NetworkVO network) {
        if (network == null) {
            HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UserVO user = findUserFromAuth(principal);
            if (user != null) {
                Set<NetworkVO> userNetworks = userService.findUserWithNetworks(user.getId()).getNetworks();
                if (userNetworks.isEmpty()) {
                    throw new HiveException(Messages.NO_NETWORKS_ASSIGNED_TO_USER, PRECONDITION_FAILED.getStatusCode());
                }
                return userNetworks.iterator().next();
            }
        }
        return network;
    }

    private UserVO findUserFromAuth(HivePrincipal principal) {
        return principal.getUser();
    }

}

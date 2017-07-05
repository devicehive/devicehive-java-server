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
import com.devicehive.exceptions.ActionNotAllowedException;
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
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.*;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;

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
    private TimestampService timestampService;
    @Autowired
    private DeviceDao deviceDao;
    @Autowired
    private RpcClient rpcClient;

    //todo equipmentSet is not used
    @Transactional(propagation = Propagation.REQUIRED)
    public void deviceSaveAndNotify(DeviceUpdate device, HivePrincipal principal) {
        logger.debug("Device: {}. Current principal: {}.", device.getId(), principal == null ? null : principal.getName());

        boolean principalHasUserAndAuthenticated = principal != null && principal.getUser() != null && principal.isAuthenticated();
        if (!principalHasUserAndAuthenticated) {
        	throw new HiveException(Messages.UNAUTHORIZED_REASON_PHRASE, UNAUTHORIZED.getStatusCode());
        }

        DeviceNotification dn = deviceSaveByUser(device, principal.getUser());
        dn.setTimestamp(timestampService.getDate());
        deviceNotificationService.insert(dn, device.convertTo());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    private DeviceNotification deviceSaveByUser(DeviceUpdate deviceUpdate, UserVO user) {
        logger.debug("Device save executed for device: id {}, user: {}", deviceUpdate.getId(), user.getId());
        //todo: rework when migration to VO will be done
        Long networkId = deviceUpdate.getNetworkId()
                .map(id -> {
                    NetworkVO networkVo = new NetworkVO();
                    networkVo.setId(id);
                    if (!userService.hasAccessToNetwork(user, networkVo)) {
                        throw new ActionNotAllowedException(Messages.NO_ACCESS_TO_NETWORK);
                    }
                    return id;
                })
                .orElseGet(() -> networkService.findDefaultNetworkByUserId(user.getId()));
        // TODO [requies a lot of details]!
        DeviceVO existingDevice = deviceDao.findById(deviceUpdate.getId().orElse(null));
        if (existingDevice == null) {
            DeviceVO device = deviceUpdate.convertTo();
            device.setNetworkId(networkId);
            if (device.getBlocked() == null) {
                device.setBlocked(false);
            }
            deviceDao.persist(device);
            return ServerResponsesFactory.createNotificationForDevice(device, SpecialNotifications.DEVICE_ADD);
        } else {
            if (!userService.hasAccessToDevice(user, existingDevice.getDeviceId())) {
                logger.error("User {} has no access to device {}", user.getId(), existingDevice.getId());
                throw new HiveException(Messages.NO_ACCESS_TO_DEVICE, FORBIDDEN.getStatusCode());
            }
            
            existingDevice.setData(deviceUpdate.getData().orElse(null));
            
            if (deviceUpdate.getNetworkId().isPresent()){
                existingDevice.setNetworkId(networkId);
            }
            if (deviceUpdate.getName().isPresent()){
                existingDevice.setName(deviceUpdate.getName().get());
            }
            if (deviceUpdate.getBlocked().isPresent()){
                existingDevice.setBlocked(deviceUpdate.getBlocked().get());
            }
            deviceDao.merge(existingDevice);
            return ServerResponsesFactory.createNotificationForDevice(existingDevice, SpecialNotifications.DEVICE_UPDATE);
        }
    }

    @Transactional
    public DeviceNotification deviceSave(DeviceUpdate deviceUpdate) {
        logger.debug("Device save executed for device update: id {}", deviceUpdate.getId());
        Long networkId = deviceUpdate.getNetworkId().isPresent() ? deviceUpdate.getNetworkId().get() : null;
        //TODO [requires a lot of details]
        DeviceVO existingDevice = deviceDao.findById(deviceUpdate.getId().orElse(null));

        if (existingDevice == null) {
            DeviceVO device = deviceUpdate.convertTo();
            device.setNetworkId(networkId);
            deviceDao.persist(device);
            return ServerResponsesFactory.createNotificationForDevice(device, SpecialNotifications.DEVICE_ADD);
        } else {
            if (deviceUpdate.getData().isPresent()){
                existingDevice.setData(deviceUpdate.getData().get());
            }
            if (deviceUpdate.getNetworkId().isPresent()){
                existingDevice.setNetworkId(networkId);
            }
            if (deviceUpdate.getBlocked().isPresent()){
                existingDevice.setBlocked(deviceUpdate.getBlocked().get());
            }
            deviceDao.merge(existingDevice);
            return ServerResponsesFactory.createNotificationForDevice(existingDevice, SpecialNotifications.DEVICE_UPDATE);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceVO findByIdWithPermissionsCheck(String deviceId, HivePrincipal principal) {
        List<DeviceVO> result = findByIdWithPermissionsCheck(Collections.singletonList(deviceId), principal);
        return result.isEmpty() ? null : result.get(0);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<DeviceVO> findByIdWithPermissionsCheck(Collection<String> deviceIds, HivePrincipal principal) {
        return getDeviceList(new ArrayList<>(deviceIds), principal);
    }

    @Transactional(readOnly = true)
    public DeviceVO findById(String deviceId) {
        DeviceVO device = deviceDao.findById(deviceId);

        if (device == null) {
            logger.error("Device with ID {} not found", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), NOT_FOUND.getStatusCode());
        }
        return device;
    }

    //TODO: only migrated to genericDAO, need to migrate Device PK to DeviceId and use directly GenericDAO#remove
    @Transactional
    public boolean deleteDevice(@NotNull String deviceId) {
        return deviceDao.deleteById(deviceId) != 0;
    }

    //@Transactional(readOnly = true)
    public CompletableFuture<List<DeviceVO>> list(String name,
                                                 String namePattern,
                                                 Long networkId,
                                                 String networkName,
                                                 String sortField,
                                                 boolean sortOrderAsc,
                                                 Integer take,
                                                 Integer skip,
                                                 HivePrincipal principal) {
        ListDeviceRequest request = new ListDeviceRequest();
        request.setName(name);
        request.setNamePattern(namePattern);
        request.setNetworkId(networkId);
        request.setNetworkName(networkName);
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
    public long getAllowedDevicesCount(HivePrincipal principal, List<String> deviceIds) {
        return deviceDao.getAllowedDeviceCount(principal, deviceIds);
    }

    private List<DeviceVO> getDeviceList(List<String> deviceIds, HivePrincipal principal) {
        return deviceDao.getDeviceList(deviceIds, principal);
    }

    public Set<String> getDeviceIds(HivePrincipal principal) {
        Set<String> deviceIds = principal.getDeviceIds();
        if (principal.areAllDevicesAvailable()) {
            try {
                deviceIds = list(null, null, null, null,
                        null,false, null, null, principal)
                        .get()
                        .stream()
                        .map(deviceVO -> deviceVO.getDeviceId())
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                logger.error(Messages.INTERNAL_SERVER_ERROR, e);
                throw new HiveException(Messages.INTERNAL_SERVER_ERROR, SC_INTERNAL_SERVER_ERROR);
            }
        }
        return deviceIds;
    }
}

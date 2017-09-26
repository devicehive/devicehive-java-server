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
import com.devicehive.model.rpc.DeviceCreateRequest;
import com.devicehive.model.rpc.ListDeviceRequest;
import com.devicehive.model.rpc.ListDeviceResponse;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.vo.*;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.ws.rs.core.Response.Status.*;

@Component
public class DeviceService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceNotificationService deviceNotificationService;
    private final NetworkService networkService;
    private final UserService userService;
    private final TimestampService timestampService;
    private final DeviceDao deviceDao;
    private final RpcClient rpcClient;

    @Autowired
    public DeviceService(DeviceNotificationService deviceNotificationService,
                         NetworkService networkService,
                         UserService userService,
                         TimestampService timestampService,
                         DeviceDao deviceDao,
                         RpcClient rpcClient) {
        this.deviceNotificationService = deviceNotificationService;
        this.networkService = networkService;
        this.userService = userService;
        this.timestampService = timestampService;
        this.deviceDao = deviceDao;
        this.rpcClient = rpcClient;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deviceSaveAndNotify(DeviceUpdate device, HivePrincipal principal) {
        logger.debug("Device: {}. Current principal: {}.", device.getId(), principal == null ? null : principal.getName());

        boolean principalHasUserAndAuthenticated = principal != null && principal.getUser() != null && principal.isAuthenticated();
        if (!principalHasUserAndAuthenticated) {
        	throw new HiveException(Messages.UNAUTHORIZED_REASON_PHRASE, UNAUTHORIZED.getStatusCode());
        }
        DeviceVO oldDevice = deviceDao.findById(device.getId().orElse(null));

        DeviceNotification dn = deviceSaveByUser(device, principal.getUser());
        dn.setTimestamp(timestampService.getDate());
        deviceNotificationService.insert(dn, device.convertTo());

        DeviceCreateRequest deviceCreateRequest = new DeviceCreateRequest(findById(device.getId().orElse(null)),
                oldDevice != null? oldDevice.getNetworkId() : null);
        Request request = Request.newBuilder()
                .withBody(deviceCreateRequest)
                .build();
        Consumer<Response> responseConsumer = response -> {
            Action resAction = response.getBody().getAction();
            CompletableFuture<String> future = new CompletableFuture<>();
            if (resAction.equals(Action.DEVICE_CREATE_RESPONSE)) {
                future.complete(response.getBody().getAction().name());
            } else {
                logger.warn("Unknown action received from backend {}", resAction);
            }
        };
        rpcClient.call(request, responseConsumer);
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

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceVO findByIdWithPermissionsCheckIfExists(String deviceId, HivePrincipal principal) {
        DeviceVO deviceVO = findByIdWithPermissionsCheck(deviceId, principal);

        if (deviceVO == null) {
            logger.error("Device with ID {} not found", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), NOT_FOUND.getStatusCode());
        }
        return deviceVO;
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

    public CompletableFuture<List<DeviceVO>> list(ListDeviceRequest request) {
        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request.newBuilder().withBody(request).build(), new ResponseConsumer(future));
        return future.thenApply(r -> ((ListDeviceResponse) r.getBody()).getDevices());
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    //TODO: need to remove it
    public long getAllowedDevicesCount(HivePrincipal principal, List<String> deviceIds) {
        return deviceDao.getAllowedDeviceCount(principal, deviceIds);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<DeviceVO> getAllowedExistingDevices(Set<String> deviceIds, HivePrincipal principal) {
        List<DeviceVO> devices = findByIdWithPermissionsCheck(deviceIds, principal);
        Set<String> allowedIds = devices.stream()
                .map(deviceVO -> deviceVO.getDeviceId())
                .collect(Collectors.toSet());

        Set<String> unresolvedIds = Sets.difference(deviceIds, allowedIds);
        if (unresolvedIds.isEmpty()) {
            return devices;
        }

        Set<String> forbiddedIds = unresolvedIds.stream()
                .filter(deviceId -> !principal.hasAccessToDevice(deviceId))
                .collect(Collectors.toSet());
        if (forbiddedIds.isEmpty()) {
            throw new HiveException(String.format(Messages.DEVICES_NOT_FOUND, unresolvedIds), SC_NOT_FOUND);
        }

        throw new HiveException(Messages.ACCESS_DENIED, SC_FORBIDDEN);

        
    }

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

    private List<DeviceVO> getDeviceList(List<String> deviceIds, HivePrincipal principal) {
        return deviceDao.getDeviceList(deviceIds, principal);
    }
    
}

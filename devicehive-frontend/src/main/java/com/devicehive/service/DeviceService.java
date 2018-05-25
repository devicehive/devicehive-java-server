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
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.response.EntityCountResponse;
import com.devicehive.model.rpc.*;
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
public class DeviceService extends BaseDeviceService {
    private static final Logger logger = LoggerFactory.getLogger(BaseDeviceService.class);

    private final DeviceNotificationService deviceNotificationService;
    private final BaseNetworkService networkService;
    private final DeviceTypeService deviceTypeService;
    private final UserService userService;
    private final TimestampService timestampService;

    @Autowired
    public DeviceService(DeviceNotificationService deviceNotificationService,
                         BaseNetworkService networkService,
                         DeviceTypeService deviceTypeService,
                         UserService userService,
                         TimestampService timestampService,
                         DeviceDao deviceDao,
                         RpcClient rpcClient) {
        super(deviceDao, networkService, rpcClient);
        this.deviceNotificationService = deviceNotificationService;
        this.networkService = networkService;
        this.deviceTypeService = deviceTypeService;
        this.userService = userService;
        this.timestampService = timestampService;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deviceSaveAndNotify(String deviceId, DeviceUpdate device, HivePrincipal principal) {
        logger.debug("Device: {}. Current principal: {}.", deviceId, principal == null ? null : principal.getName());

        boolean principalHasUserAndAuthenticated = principal != null && principal.getUser() != null && principal.isAuthenticated();
        if (!principalHasUserAndAuthenticated) {
        	throw new HiveException(Messages.UNAUTHORIZED_REASON_PHRASE, UNAUTHORIZED.getStatusCode());
        }

        DeviceNotification dn = deviceSaveByUser(deviceId, device, principal);
        dn.setTimestamp(timestampService.getDate());
        deviceNotificationService.insert(dn, device.convertTo(deviceId));
    }

    @Transactional
    public DeviceNotification deviceSave(String deviceId, DeviceUpdate deviceUpdate) {
        logger.debug("Device save executed for device update: id {}", deviceId);
        Long networkId = deviceUpdate.getNetworkId().isPresent() ? deviceUpdate.getNetworkId().get() : null;
        //TODO [requires a lot of details]
        DeviceVO existingDevice = deviceDao.findById(deviceId);

        if (existingDevice == null) {
            DeviceVO device = deviceUpdate.convertTo(deviceId);
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
        DeviceVO deviceVO = deviceDao.findById(deviceId);
        if (deviceVO == null) {
            logger.error("Device with ID {} not found", deviceId);
            return false;
        }

        DeviceDeleteRequest deviceDeleteRequest = new DeviceDeleteRequest(deviceVO);

        Request request = Request.newBuilder()
                .withBody(deviceDeleteRequest)
                .build();

        CompletableFuture<String> future = new CompletableFuture<>();
        Consumer<Response> responseConsumer = response -> {
            Action resAction = response.getBody().getAction();
            if (resAction.equals(Action.DEVICE_DELETE_RESPONSE)) {
                future.complete(response.getBody().getAction().name());
            } else {
                logger.warn("Unknown action received from backend {}", resAction);
            }
        };

        rpcClient.call(request, responseConsumer);

        return deviceDao.deleteById(deviceId) != 0;
    }

    public CompletableFuture<EntityCountResponse> count(String name, String namePattern, Long networkId, String networkName, HivePrincipal principal) {
        CountDeviceRequest countDeviceRequest = new CountDeviceRequest(name, namePattern, networkId, networkName, principal);

        return count(countDeviceRequest);
    }

    public CompletableFuture<EntityCountResponse> count(CountDeviceRequest countDeviceRequest) {
        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request
                .newBuilder()
                .withBody(countDeviceRequest)
                .build(), new ResponseConsumer(future));

        return future.thenApply(response -> new EntityCountResponse((CountResponse)response.getBody()));
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<DeviceVO> getAllowedExistingDevices(Set<String> deviceIds, HivePrincipal principal) {
        List<DeviceVO> devices = findByIdWithPermissionsCheck(deviceIds, principal);
        Set<String> allowedIds = devices.stream()
                .map(DeviceVO::getDeviceId)
                .collect(Collectors.toSet());

        Set<String> unresolvedIds = Sets.difference(deviceIds, allowedIds);
        if (unresolvedIds.isEmpty()) {
            return devices;
        }

        if (UserRole.ADMIN.equals(principal.getUser().getRole())) {
            throw new HiveException(String.format(Messages.DEVICES_NOT_FOUND, unresolvedIds), SC_NOT_FOUND);
        } else {
            throw new HiveException(Messages.ACCESS_DENIED, SC_FORBIDDEN);
        }
    }

    private DeviceNotification deviceSaveByUser(String deviceId, DeviceUpdate deviceUpdate, HivePrincipal principal) {
        UserVO user = principal.getUser();
        logger.debug("Device save executed for device: id {}, user: {}", deviceId, user.getId());
        //todo: rework when migration to VO will be done
        Long networkId = deviceUpdate.getNetworkId()
                .map(id -> {
                    NetworkVO networkVo = new NetworkVO();
                    networkVo.setId(id);
                    if (!networkService.isNetworkExists(id)) {
                        throw new IllegalParametersException(Messages.INVALID_REQUEST_PARAMETERS);
                    }
                    if (!userService.hasAccessToNetwork(user, networkVo)) {
                        throw new ActionNotAllowedException(Messages.NO_ACCESS_TO_NETWORK);
                    }
                    return id;
                })
                .orElseGet(() -> networkService.findDefaultNetworkByUserId(user.getId()));
        Long deviceTypeId = deviceUpdate.getDeviceTypeId()
                .map(id -> {
                    if (!deviceTypeService.isDeviceTypeExists(id)) {
                        throw new IllegalParametersException(Messages.INVALID_REQUEST_PARAMETERS);
                    }
                    return id;
                }).orElseGet(() -> {
                    if (principal.areAllDeviceTypesAvailable() || (principal.getDeviceTypeIds() != null && !principal.getDeviceTypeIds().isEmpty())) {
                        return deviceTypeService.findDefaultDeviceType(principal.getDeviceTypeIds());
                    } else {
                        throw new ActionNotAllowedException(Messages.NO_ACCESS_TO_DEVICE_TYPE);
                    }
                });
        // TODO [requies a lot of details]!
        DeviceVO existingDevice = deviceDao.findById(deviceId);
        if (existingDevice == null) {
            DeviceVO device = deviceUpdate.convertTo(deviceId);
            device.setNetworkId(networkId);
            device.setDeviceTypeId(deviceTypeId);
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
            if (deviceUpdate.getDeviceTypeId().isPresent()){
                existingDevice.setDeviceTypeId(deviceTypeId);
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

}

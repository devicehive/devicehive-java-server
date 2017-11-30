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

import static javax.ws.rs.core.Response.Status.*;

@Component
public class DeviceService extends BaseDeviceService {
    private static final Logger logger = LoggerFactory.getLogger(BaseDeviceService.class);

    private final DeviceNotificationService deviceNotificationService;
    private final UserService userService;
    private final TimestampService timestampService;
    private final RpcClient rpcClient;

    @Autowired
    public DeviceService(DeviceNotificationService deviceNotificationService,
                         NetworkService networkService,
                         UserService userService,
                         TimestampService timestampService,
                         DeviceDao deviceDao,
                         RpcClient rpcClient) {
        super(deviceDao, networkService);
        this.deviceNotificationService = deviceNotificationService;
        this.userService = userService;
        this.timestampService = timestampService;
        this.rpcClient = rpcClient;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CompletableFuture<String> deviceSaveAndNotify(String deviceId, DeviceUpdate device, HivePrincipal principal) {
        logger.debug("Device: {}. Current principal: {}.", deviceId, principal == null ? null : principal.getName());

        boolean principalHasUserAndAuthenticated = principal != null && principal.getUser() != null && principal.isAuthenticated();
        if (!principalHasUserAndAuthenticated) {
        	throw new HiveException(Messages.UNAUTHORIZED_REASON_PHRASE, UNAUTHORIZED.getStatusCode());
        }
        DeviceVO oldDevice = deviceDao.findById(deviceId);

        DeviceNotification dn = deviceSaveByUser(deviceId, device, principal.getUser());
        dn.setTimestamp(timestampService.getDate());
        deviceNotificationService.insert(dn, device.convertTo(deviceId));

        DeviceCreateRequest deviceCreateRequest = new DeviceCreateRequest(findById(deviceId),
                oldDevice != null? oldDevice.getNetworkId() : null);
        Request request = Request.newBuilder()
                .withBody(deviceCreateRequest)
                .build();
        CompletableFuture<String> future = new CompletableFuture<>();
        Consumer<Response> responseConsumer = response -> {
            Action resAction = response.getBody().getAction();
            if (resAction.equals(Action.DEVICE_CREATE_RESPONSE)) {
                future.complete(response.getBody().getAction().name());
            } else {
                logger.warn("Unknown action received from backend {}", resAction);
            }
        };
        
        rpcClient.call(request, responseConsumer);
        
        return future;
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

    public CompletableFuture<List<DeviceVO>> list(String name, String namePattern, Long networkId, String networkName,
              String sortField, String sortOrderAsc, Integer take, Integer skip, HivePrincipal principal) {

        ListDeviceRequest listDeviceRequest = new ListDeviceRequest();
        listDeviceRequest.setName(name);
        listDeviceRequest.setNamePattern(namePattern);
        listDeviceRequest.setNetworkId(networkId);
        listDeviceRequest.setNetworkName(networkName);
        listDeviceRequest.setSortField(sortField);
        listDeviceRequest.setSortOrder(sortOrderAsc);
        listDeviceRequest.setTake(take);
        listDeviceRequest.setSkip(skip);
        listDeviceRequest.setPrincipal(principal);

        return list(listDeviceRequest);
    }

    public CompletableFuture<List<DeviceVO>> list(ListDeviceRequest listDeviceRequest) {
        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request
                .newBuilder()
                .withBody(listDeviceRequest)
                .build(), new ResponseConsumer(future));

        return future.thenApply(response -> ((ListDeviceResponse) response.getBody()).getDevices());
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    //TODO: need to remove it
    public long getAllowedDevicesCount(HivePrincipal principal, List<String> deviceIds) {
        return deviceDao.getAllowedDeviceCount(principal, deviceIds);
    }

    private DeviceNotification deviceSaveByUser(String deviceId, DeviceUpdate deviceUpdate, UserVO user) {
        logger.debug("Device save executed for device: id {}, user: {}", deviceId, user.getId());
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
        DeviceVO existingDevice = deviceDao.findById(deviceId);
        if (existingDevice == null) {
            DeviceVO device = deviceUpdate.convertTo(deviceId);
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

}

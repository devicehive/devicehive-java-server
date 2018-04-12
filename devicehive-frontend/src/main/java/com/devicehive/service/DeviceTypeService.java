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

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceTypeDao;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.response.EntityCountResponse;
import com.devicehive.model.rpc.*;
import com.devicehive.model.updates.DeviceTypeUpdate;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.HiveValidator;
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

import static java.util.Optional.*;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

@Component
public class DeviceTypeService extends BaseDeviceTypeService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceTypeService.class);

    private final HiveValidator hiveValidator;

    @Autowired
    public DeviceTypeService(HiveValidator hiveValidator,
                             DeviceTypeDao deviceTypeDao,
                             RpcClient rpcClient) {
        super(deviceTypeDao, rpcClient);
        this.hiveValidator = hiveValidator;
    }

    @Transactional
    public boolean delete(long id, boolean force) {
        logger.trace("About to execute named query \"DeviceType.deleteById\" for ");
        DeviceTypeWithUsersAndDevicesVO deviceType = getWithDevices(id);
        if (!force && deviceType != null && !deviceType.getDevices().isEmpty()) {
            logger.warn("Failed to delete non-empty device type with id {}", id);
            String deviceIds = deviceType.getDevices().stream().map(DeviceVO::getDeviceId).collect(Collectors.joining(", "));
            throw new HiveException(String.format(Messages.DEVICE_TYPE_DELETION_NOT_ALLOWED, deviceIds), SC_BAD_REQUEST);
        }
        int result = deviceTypeDao.deleteById(id);
        logger.debug("Deleted {} rows from DeviceType table", result);
        return result > 0;
    }

    @Transactional
    public DeviceTypeVO create(DeviceTypeVO newDeviceType) {
        hiveValidator.validate(newDeviceType);
        logger.debug("Creating device type {}", newDeviceType);
        if (newDeviceType.getId() != null) {
            logger.error("Can't create device type entity with id={} specified", newDeviceType.getId());
            throw new IllegalParametersException(Messages.ID_NOT_ALLOWED);
        }
        List<DeviceTypeVO> existing = deviceTypeDao.findByName(newDeviceType.getName());
        if (!existing.isEmpty()) {
            logger.error("Device type with name {} already exists", newDeviceType.getName());
            throw new ActionNotAllowedException(Messages.DUPLICATE_DEVICE_TYPE);
        }
        deviceTypeDao.persist(newDeviceType);
        logger.info("Entity {} created successfully", newDeviceType);
        return newDeviceType;
    }

    @Transactional
    public DeviceTypeVO update(@NotNull Long deviceTypeId, DeviceTypeUpdate deviceTypeUpdate) {
        DeviceTypeVO existing = deviceTypeDao.find(deviceTypeId);
        if (existing == null) {
            throw new NoSuchElementException(String.format(Messages.DEVICE_TYPE_NOT_FOUND, deviceTypeId));
        }
        if (deviceTypeUpdate.getName().isPresent()) {
            existing.setName(deviceTypeUpdate.getName().get());
        }
        if (deviceTypeUpdate.getDescription().isPresent()) {
            existing.setDescription(deviceTypeUpdate.getDescription().get());
        }
        hiveValidator.validate(existing);

        return deviceTypeDao.merge(existing);
    }

    public CompletableFuture<List<DeviceTypeVO>> listAll() {
        final ListDeviceTypeRequest request = new ListDeviceTypeRequest();

        return list(request);
    }

    public CompletableFuture<EntityCountResponse> count(String name, String namePattern, HivePrincipal principal) {
        Optional<HivePrincipal> principalOpt = ofNullable(principal);
        CountDeviceTypeRequest countDeviceTypeRequest = new CountDeviceTypeRequest(name, namePattern, principalOpt);

        return count(countDeviceTypeRequest);
    }

    public CompletableFuture<EntityCountResponse> count(CountDeviceTypeRequest countDeviceTypeRequest) {
        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request
                .newBuilder()
                .withBody(countDeviceTypeRequest)
                .build(), new ResponseConsumer(future));

        return future.thenApply(response -> new EntityCountResponse((CountResponse)response.getBody()));
    }

    @Transactional
    public DeviceTypeVO verifyDeviceType(Optional<DeviceTypeVO> deviceTypeNullable) {
        //case device type is not defined
        if (deviceTypeNullable == null || deviceTypeNullable.orElse(null) == null) {
            return null;
        }
        DeviceTypeVO deviceType = deviceTypeNullable.get();

        Optional<DeviceTypeVO> storedOpt = findDeviceTypeByIdOrName(deviceType);
        if (storedOpt.isPresent()) {
            return storedOpt.get();
        }

        throw new NoSuchElementException(String.format(Messages.DEVICE_TYPE_NOT_FOUND, deviceType.getId()));
    }

    @Transactional
    public DeviceTypeVO createOrUpdateDeviceTypeByUser(Optional<DeviceTypeVO> deviceTypeNullable, UserVO user) {
        //case device type is not defined
        if (deviceTypeNullable == null || deviceTypeNullable.orElse(null) == null) {
            return null;
        }

        DeviceTypeVO deviceType = deviceTypeNullable.orElse(null);

        Optional<DeviceTypeVO> storedOpt = findDeviceTypeByIdOrName(deviceType);
        if (storedOpt.isPresent()) {
            return storedOpt.get();
        } else {
            if (deviceType.getId() != null) {
                throw new IllegalParametersException(Messages.INVALID_REQUEST_PARAMETERS);
            }
            if (user.isAdmin()) {
                DeviceTypeWithUsersAndDevicesVO newDeviceType = new DeviceTypeWithUsersAndDevicesVO(deviceType);
                deviceTypeDao.persist(newDeviceType);
                deviceType.setId(newDeviceType.getId());
            } else {
                throw new ActionNotAllowedException(Messages.DEVICE_TYPE_CREATION_NOT_ALLOWED);
            }
            return deviceType;
        }
    }

    @Transactional
    public Long findDefaultDeviceType(Set<Long> deviceTypeIds) {
        return deviceTypeDao.findDefault(deviceTypeIds)
                .map(DeviceTypeVO::getId)
                .orElseThrow(() -> new ActionNotAllowedException(Messages.NO_ACCESS_TO_DEVICE_TYPE));
    }

    @Transactional
    public DeviceTypeVO createOrUpdateDeviceTypeByUser(UserVO user) {
        DeviceTypeVO deviceTypeVO = new DeviceTypeVO();
        deviceTypeVO.setName(user.getLogin());
        deviceTypeVO.setDescription(String.format("User %s default device type", user.getLogin()));
        return createOrUpdateDeviceTypeByUser(Optional.ofNullable(deviceTypeVO), user);
    }

    public boolean isDeviceTypeExists(Long deviceTypeId) {
        return ofNullable(deviceTypeId)
                .map(id -> deviceTypeDao.find(id) != null)
                .orElse(false);
    }

    private Optional<DeviceTypeVO> findDeviceTypeByIdOrName(DeviceTypeVO deviceType) {
        return ofNullable(deviceType.getId())
                .map(id -> ofNullable(deviceTypeDao.find(id)))
                .orElseGet(() -> deviceTypeDao.findFirstByName(deviceType.getName()));
    }
}

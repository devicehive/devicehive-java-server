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

import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceClassDao;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.rpc.ListDeviceClassRequest;
import com.devicehive.model.rpc.ListDeviceClassResponse;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.DeviceClassVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Component
public class DeviceClassService {

    @Autowired
    private HiveValidator hiveValidator;
    @Autowired
    private DeviceClassDao deviceClassDao;
    @Autowired
    private RpcClient rpcClient;

    @Transactional
    public void delete(@NotNull long id) {
        deviceClassDao.remove(id);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceClassVO getWithEquipment(@NotNull Long id) {
        return deviceClassDao.find(id);
    }

    @Transactional
    public DeviceClassVO createOrUpdateDeviceClass(Optional<DeviceClassUpdate> deviceClass) {
        DeviceClassVO stored;
        //use existing
        if (deviceClass == null || !deviceClass.isPresent()) {
            return null;
        }
        //check is already done
        DeviceClassUpdate deviceClassUpdate = deviceClass.orElse(null);
        DeviceClassVO deviceClassFromMessage = deviceClassUpdate.convertTo();
        if (deviceClassFromMessage.getId() != null) {
            stored = deviceClassDao.find(deviceClassFromMessage.getId());
        } else {
            stored = deviceClassDao.findByName(deviceClassFromMessage.getName());
        }
        if (stored != null) {
            if (!stored.getIsPermanent()) {
                update(stored.getId(), deviceClassUpdate);
            }
            return stored;
        } else {
            return addDeviceClass(deviceClassFromMessage);
        }
    }

    @Transactional
    public DeviceClassVO addDeviceClass(DeviceClassVO deviceClass) {
        if (deviceClassDao.findByName(deviceClass.getName()) != null) {
            throw new HiveException(Messages.DEVICE_CLASS_WITH_SUCH_NAME_AND_VERSION_EXISTS, FORBIDDEN.getStatusCode());
        }
        if (deviceClass.getIsPermanent() == null) {
            deviceClass.setIsPermanent(false);
        }
        hiveValidator.validate(deviceClass);
        deviceClass = deviceClassDao.persist(deviceClass);
        return deviceClass;
    }

    @Transactional
    public DeviceClassVO update(@NotNull Long id, DeviceClassUpdate update) {
        DeviceClassVO stored = deviceClassDao.find(id);
        if (stored == null) {
            throw new HiveException(String.format(Messages.DEVICE_CLASS_NOT_FOUND, id), Response.Status.NOT_FOUND.getStatusCode());
        }
        if (update == null) {
            return null;
        }
        if (update.getData() != null) {
            stored.setData(update.getData().orElse(null));
        }
        if (update.getId() != null) {
            stored.setId(update.getId());
        }
        if (update.getName() != null) {
            stored.setName(update.getName().orElse(null));
        }
        if (update.getPermanent() != null) {
            stored.setIsPermanent(update.getPermanent().orElse(null));
        }
        hiveValidator.validate(stored);
        return deviceClassDao.merge(stored);
    }

    //@Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CompletableFuture<List<DeviceClassVO>> list(String name, String namePattern, String sortField,
                                                                   Boolean sortOrderAsc, Integer take, Integer skip) {
        ListDeviceClassRequest request = new ListDeviceClassRequest();
        request.setName(name);
        request.setNamePattern(namePattern);
        request.setSortField(sortField);
        request.setSortOrderAsc(sortOrderAsc);
        request.setTake(take);
        request.setSkip(skip);

        CompletableFuture<com.devicehive.shim.api.Response> future = new CompletableFuture<>();

        rpcClient.call(Request
                .newBuilder()
                .withBody(request)
                .build(), new ResponseConsumer(future));

        return future.thenApply(r -> ((ListDeviceClassResponse) r.getBody()).getDeviceClasses());
    }
}

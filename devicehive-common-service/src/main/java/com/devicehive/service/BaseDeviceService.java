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
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.rpc.ListDeviceRequest;
import com.devicehive.model.rpc.ListDeviceResponse;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.vo.DeviceVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.devicehive.configuration.Messages.ACCESS_DENIED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Component
public class BaseDeviceService {
    private static final Logger logger = LoggerFactory.getLogger(BaseDeviceService.class);

    protected final DeviceDao deviceDao;
    protected final BaseNetworkService networkService;
    protected final RpcClient rpcClient;

    @Autowired
    public BaseDeviceService(DeviceDao deviceDao,
                             BaseNetworkService networkService,
                             RpcClient rpcClient) {
        this.deviceDao = deviceDao;
        this.networkService = networkService;
        this.rpcClient = rpcClient;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceVO findByIdWithPermissionsCheck(String deviceId, HivePrincipal principal) {
        List<DeviceVO> result = findByIdWithPermissionsCheck(Collections.singletonList(deviceId), principal);
        return result.isEmpty() ? null : result.get(0);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceVO findByIdWithPermissionsCheckIfExists(String deviceId, HivePrincipal principal) {
        if (deviceId.isEmpty()) {
            logger.error("Device ID is empty");
            throw new HiveException(String.format(Messages.DEVICE_ID_REQUIRED, deviceId), BAD_REQUEST.getStatusCode());
        }
        DeviceVO deviceVO = findByIdWithPermissionsCheck(deviceId, principal);

        if (deviceVO == null) {
            logger.error("Device with ID {} not found", deviceId);
            if (UserRole.CLIENT.equals(principal.getUser().getRole())) {
                throw new HiveException(ACCESS_DENIED, SC_FORBIDDEN);
            }
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), NOT_FOUND.getStatusCode());
        }
        return deviceVO;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<DeviceVO> findByIdWithPermissionsCheck(Collection<String> deviceIds, HivePrincipal principal) {
        return getDeviceList(new ArrayList<>(deviceIds), principal);
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

    private List<DeviceVO> getDeviceList(List<String> deviceIds, HivePrincipal principal) {
        return deviceDao.getDeviceList(deviceIds, principal);
    }
}

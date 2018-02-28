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
import com.devicehive.dao.NetworkDao;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.response.EntityCountResponse;
import com.devicehive.model.rpc.*;
import com.devicehive.model.updates.NetworkUpdate;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.devicehive.configuration.Messages.NETWORKS_NOT_FOUND;
import static java.util.Optional.*;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
public class NetworkService extends BaseNetworkService {

    private static final Logger logger = LoggerFactory.getLogger(BaseNetworkService.class);


    @Autowired
    public NetworkService(HiveValidator hiveValidator,
                          NetworkDao networkDao,
                          RpcClient rpcClient) {
        super(hiveValidator, networkDao, rpcClient);
    }

    @Transactional
    public NetworkVO create(NetworkVO newNetwork) {
        hiveValidator.validate(newNetwork);
        logger.debug("Creating network {}", newNetwork);
        if (newNetwork.getId() != null) {
            logger.error("Can't create network entity with id={} specified", newNetwork.getId());
            throw new IllegalParametersException(Messages.ID_NOT_ALLOWED);
        }
        List<NetworkVO> existing = networkDao.findByName(newNetwork.getName());
        if (!existing.isEmpty()) {
            logger.error("Network with name {} already exists", newNetwork.getName());
            throw new ActionNotAllowedException(Messages.DUPLICATE_NETWORK);
        }
        networkDao.persist(newNetwork);
        logger.info("Entity {} created successfully", newNetwork);
        return newNetwork;
    }

    @Transactional
    public NetworkVO update(@NotNull Long networkId, NetworkUpdate networkUpdate) {
        NetworkVO existing = networkDao.find(networkId);
        if (existing == null) {
            throw new NoSuchElementException(String.format(Messages.NETWORK_NOT_FOUND, networkId));
        }
        if (networkUpdate.getName().isPresent()){
            existing.setName(networkUpdate.getName().get());
        }
        if (networkUpdate.getDescription().isPresent()){
            existing.setDescription(networkUpdate.getDescription().get());
        }
        hiveValidator.validate(existing);

        return networkDao.merge(existing);
    }

    public CompletableFuture<EntityCountResponse> count(String name, String namePattern, HivePrincipal principal) {
        CountNetworkRequest countNetworkRequest = new CountNetworkRequest(name, namePattern, principal);

        return count(countNetworkRequest);
    }

    public CompletableFuture<EntityCountResponse> count(CountNetworkRequest countNetworkRequest) {
        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request
                .newBuilder()
                .withBody(countNetworkRequest)
                .build(), new ResponseConsumer(future));

        return future.thenApply(response -> new EntityCountResponse((CountResponse)response.getBody()));
    }

    @Transactional
    public NetworkVO verifyNetwork(Optional<NetworkVO> networkNullable) {
        //case network is not defined
        if (networkNullable == null || networkNullable.orElse(null) == null) {
            return null;
        }
        NetworkVO network = networkNullable.get();

        Optional<NetworkVO> storedOpt = findNetworkByIdOrName(network);
        if (storedOpt.isPresent()) {
            return storedOpt.get();
        }

        throw new NoSuchElementException(String.format(Messages.NETWORK_NOT_FOUND, network.getId()));
    }

    private Optional<NetworkVO> findNetworkByIdOrName(NetworkVO network) {
        return ofNullable(network.getId())
                .map(id -> ofNullable(networkDao.find(id)))
                .orElseGet(() -> networkDao.findFirstByName(network.getName()));
    }
}

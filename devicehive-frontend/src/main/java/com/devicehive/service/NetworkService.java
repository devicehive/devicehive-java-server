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
import static com.devicehive.configuration.Constants.ALLOW_NETWORK_AUTO_CREATE;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.NetworkDao;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.rpc.ListNetworkRequest;
import com.devicehive.model.rpc.ListNetworkResponse;
import com.devicehive.model.updates.NetworkUpdate;
import com.devicehive.service.configuration.ConfigurationService;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Optional.*;

@Component
public class NetworkService {

    private static final Logger logger = LoggerFactory.getLogger(NetworkService.class);
    @Autowired
    private UserService userService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private HiveValidator hiveValidator;
    @Autowired
    private NetworkDao networkDao;
    @Autowired
    private RpcClient rpcClient;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public NetworkWithUsersAndDevicesVO getWithDevicesAndDeviceClasses(@NotNull Long networkId, @NotNull HiveAuthentication hiveAuthentication) {
        HivePrincipal principal = (HivePrincipal) hiveAuthentication.getPrincipal();

        Set<Long> permittedNetworks = principal.getNetworkIds();
        Set<String> permittedDevices = principal.getDeviceGuids();

        Optional<NetworkWithUsersAndDevicesVO> result = of(principal)
                .flatMap(pr -> {
                    if (pr.getUser() != null) {
                        return of(pr.getUser());
                    } else {
                        return empty();
                    }
                }).flatMap(user -> {
            Long idForFiltering = user.isAdmin() ? null : user.getId();
            List<NetworkWithUsersAndDevicesVO> found = networkDao.getNetworksByIdsAndUsers(idForFiltering,
                    Collections.singleton(networkId), permittedNetworks);
            return found.stream().findFirst();
        }).map(network -> {
            //fixme - important, restore functionality once permission evaluator is switched to jwt
            /*if (principal.getKey() != null) {
                        Set<AccessKeyPermissionVO> permissions = principal.getKey().getPermissions();
                        Set<AccessKeyPermissionVO> filtered = CheckPermissionsHelper
                                .filterPermissions(principal.getKey(), permissions, AccessKeyAction.GET_DEVICE,
                                        details.getClientInetAddress(), details.getOrigin());
                        if (filtered.isEmpty()) {
                            network.setDevices(Collections.emptySet());
                        }
                    }*/
            if (permittedDevices != null && !permittedDevices.isEmpty()) {
                Set<DeviceVO> allowed = network.getDevices().stream()
                        .filter(device -> permittedDevices.contains(device.getGuid()))
                        .collect(Collectors.toSet());
                network.setDevices(allowed);
            }
            return network;
        });

        return result.orElse(null);
    }

    @Transactional
    public boolean delete(long id) {
        logger.trace("About to execute named query \"Network.deleteById\" for ");
        int result = networkDao.deleteById(id);
        logger.debug("Deleted {} rows from Network table", result);
        return result > 0;
    }

    @Transactional
    public NetworkVO create(NetworkVO newNetwork) {
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
        hiveValidator.validate(newNetwork);
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
        if (networkUpdate.getKey().isPresent()){
            existing.setKey(networkUpdate.getKey().get());
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

    //@Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CompletableFuture<List<NetworkVO>> list(String name,
            String namePattern,
            String sortField,
            boolean sortOrderAsc,
            Integer take,
            Integer skip,
            HivePrincipal principal) {
        Optional<HivePrincipal> principalOpt = ofNullable(principal);

        ListNetworkRequest request = new ListNetworkRequest();
        request.setName(name);
        request.setNamePattern(namePattern);
        request.setSortField(sortField);
        request.setSortOrderAsc(sortOrderAsc);
        request.setTake(take);
        request.setSkip(skip);
        request.setPrincipal(principalOpt);

        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request.newBuilder().withBody(request).build(), new ResponseConsumer(future));

        return future.thenApply(r -> ((ListNetworkResponse) r.getBody()).getNetworks());
    }

    @Transactional
    public NetworkVO createOrVerifyNetwork(Optional<NetworkVO> networkNullable) {
        //case network is not defined
        if (networkNullable == null || networkNullable.orElse(null) == null) {
            return null;
        }
        NetworkVO network = networkNullable.get();

        Optional<NetworkVO> storedOpt = findNetworkByIdOrName(network);
        if (storedOpt.isPresent()) {
            return validateNetworkKey(storedOpt.get(), network);
        } else {
            if (network.getId() != null) {
                throw new IllegalParametersException(Messages.INVALID_REQUEST_PARAMETERS);
            }
            boolean allowed = configurationService.getBoolean(ALLOW_NETWORK_AUTO_CREATE, false);
            if (allowed) {
                NetworkWithUsersAndDevicesVO newNetwork = new NetworkWithUsersAndDevicesVO(network);
                networkDao.persist(newNetwork);
                network.setId(newNetwork.getId());
            }
            return network;
        }
    }

    @Transactional
    public NetworkVO createOrUpdateNetworkByUser(Optional<NetworkVO> networkNullable, UserVO user) {
        //case network is not defined
        if (networkNullable == null || networkNullable.orElse(null) == null) {
            return null;
        }

        NetworkVO network = networkNullable.orElse(null);

        Optional<NetworkVO> storedOpt = findNetworkByIdOrName(network);
        if (storedOpt.isPresent()) {
            NetworkVO stored = validateNetworkKey(storedOpt.get(), network);
            if (!userService.hasAccessToNetwork(user, stored)) {
                throw new ActionNotAllowedException(Messages.NO_ACCESS_TO_NETWORK);
            }
            return stored;
        } else {
            if (network.getId() != null) {
                throw new IllegalParametersException(Messages.INVALID_REQUEST_PARAMETERS);
            }
            boolean allowed = configurationService.getBoolean(ALLOW_NETWORK_AUTO_CREATE, false);
            if (user.isAdmin() || allowed) {
                NetworkWithUsersAndDevicesVO newNetwork = new NetworkWithUsersAndDevicesVO(network);
                networkDao.persist(newNetwork);
                network.setId(newNetwork.getId());
                userService.assignNetwork(user.getId(), network.getId()); // Assign created network to user
            } else {
                throw new ActionNotAllowedException(Messages.NETWORK_CREATION_NOT_ALLOWED);
            }
            return network;
        }
    }

    @Transactional
    public NetworkVO createOrUpdateNetworkByUser(UserVO user) {
        NetworkVO networkVO = new NetworkVO();
        networkVO.setKey(java.util.UUID.randomUUID().toString());
        networkVO.setName(user.getLogin());
        networkVO.setDescription(String.format("User %s default network", user.getLogin()));
        return createOrUpdateNetworkByUser(Optional.ofNullable(networkVO), user);
    }

    private Optional<NetworkVO> findNetworkByIdOrName(NetworkVO network) {
        return ofNullable(network.getId())
                .map(id -> ofNullable(networkDao.find(id)))
                .orElseGet(() -> networkDao.findFirstByName(network.getName()));
    }

    private NetworkVO validateNetworkKey(NetworkVO stored, NetworkVO received) {
        if (stored.getKey() != null && !stored.getKey().equals(received.getKey())) {
            throw new ActionNotAllowedException(Messages.INVALID_NETWORK_KEY);
        }
        return stored;
    }
}

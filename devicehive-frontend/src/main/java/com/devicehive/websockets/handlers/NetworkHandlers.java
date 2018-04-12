package com.devicehive.websockets.handlers;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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
import com.devicehive.auth.websockets.HiveWebsocketAuth;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.handler.WebSocketClientHandler;
import com.devicehive.model.rpc.CountNetworkRequest;
import com.devicehive.model.rpc.ListNetworkRequest;
import com.devicehive.model.updates.NetworkUpdate;
import com.devicehive.service.BaseNetworkService;
import com.devicehive.service.NetworkService;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Optional;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Component
public class NetworkHandlers {
    private static final Logger logger = LoggerFactory.getLogger(NetworkHandlers.class);

    private final NetworkService networkService;
    private final WebSocketClientHandler webSocketClientHandler;
    private final Gson gson;

    @Autowired
    public NetworkHandlers(NetworkService networkService, WebSocketClientHandler webSocketClientHandler, Gson gson) {
        this.networkService = networkService;
        this.webSocketClientHandler = webSocketClientHandler;
        this.gson = gson;
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_NETWORK')")
    public void processNetworkList(JsonObject request, WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ListNetworkRequest listNetworkRequest = ListNetworkRequest.createListNetworkRequest(request);
        listNetworkRequest.setPrincipal(Optional.ofNullable(principal));

        String sortField = Optional.ofNullable(listNetworkRequest.getSortField()).map(String::toLowerCase).orElse(null);
        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !NAME.equalsIgnoreCase(sortField)) {
            logger.error("Unable to proceed network list request. Invalid sortField");
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, BAD_REQUEST.getStatusCode());
        }

        WebSocketResponse response = new WebSocketResponse();
        if (!principal.areAllNetworksAvailable() && (principal.getNetworkIds() == null || principal.getNetworkIds().isEmpty())) {
            logger.warn("Unable to get list for empty networks");
            response.addValue(NETWORKS, Collections.<NetworkVO>emptyList(), NETWORKS_LISTED);
            webSocketClientHandler.sendMessage(request, response, session);
        } else {
            networkService.list(listNetworkRequest)
                    .thenAccept(networks -> {
                        logger.debug("Network list request proceed successfully.");
                        response.addValue(NETWORKS, networks, NETWORKS_LISTED);
                        webSocketClientHandler.sendMessage(request, response, session);
                    });
        }
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_NETWORK')")
    public void processNetworkCount(JsonObject request, WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CountNetworkRequest countNetworkRequest = CountNetworkRequest.createCountNetworkRequest(request, principal);

        WebSocketResponse response = new WebSocketResponse();
        networkService.count(countNetworkRequest)
                .thenAccept(count -> {
                    logger.debug("Network count request proceed successfully.");
                    response.addValue(COUNT, count.getCount(), null);
                    webSocketClientHandler.sendMessage(request, response, session);
                });
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#networkId, 'GET_NETWORK')")
    public void processNetworkGet(Long networkId, JsonObject request, WebSocketSession session) {
        logger.debug("Network get requested.");
        if (networkId == null) {
            logger.error(Messages.NETWORK_ID_REQUIRED);
            throw new HiveException(Messages.NETWORK_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        NetworkWithUsersAndDevicesVO existing = networkService.getWithDevices(networkId);
        if (existing == null) {
            logger.error(String.format(Messages.NETWORK_NOT_FOUND, networkId));
            throw new HiveException(String.format(Messages.NETWORK_NOT_FOUND, networkId), NOT_FOUND.getStatusCode());
        }

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(NETWORK, existing, NETWORK_PUBLISHED);
        webSocketClientHandler.sendMessage(request, response, session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_NETWORK')")
    public void processNetworkInsert(JsonObject request, WebSocketSession session) {
        logger.debug("Network insert requested");
        NetworkVO network = gson.fromJson(request.get(NETWORK), NetworkVO.class);
        if (network == null) {
            logger.error(Messages.NETWORK_REQUIRED);
            throw new HiveException(Messages.NETWORK_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        NetworkVO result = networkService.create(network);
        logger.debug("New network has been created");

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(NETWORK, result, NETWORK_SUBMITTED);
        webSocketClientHandler.sendMessage(request, response, session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#networkId, 'MANAGE_NETWORK')")
    public void processNetworkUpdate(Long networkId, JsonObject request, WebSocketSession session) {
        NetworkUpdate networkToUpdate = gson.fromJson(request.get(NETWORK), NetworkUpdate.class);
        logger.debug("Network update requested. Id : {}", networkId);
        if (networkId == null) {
            logger.error(Messages.NETWORK_ID_REQUIRED);
            throw new HiveException(Messages.NETWORK_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        networkService.update(networkId, networkToUpdate);
        logger.debug("Network has been updated successfully. Id : {}", networkId);
        webSocketClientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#networkId, 'MANAGE_NETWORK')")
    public void processNetworkDelete(Long networkId, JsonObject request, WebSocketSession session) {
        logger.debug("Network delete requested");
        boolean force = Optional.ofNullable(gson.fromJson(request.get(FORCE), Boolean.class)).orElse(false);
        boolean isDeleted = networkService.delete(networkId, force);
        if (!isDeleted) {
            logger.error(String.format(Messages.NETWORK_NOT_FOUND, networkId));
            throw new HiveException(String.format(Messages.NETWORK_NOT_FOUND, networkId), NOT_FOUND.getStatusCode());
        }
        logger.debug("Network with id = {} does not exists any more.", networkId);
        webSocketClientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

}

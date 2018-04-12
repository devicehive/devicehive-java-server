package com.devicehive.resource.impl;

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
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.updates.NetworkUpdate;
import com.devicehive.resource.NetworkResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.BaseNetworkService;
import com.devicehive.service.NetworkService;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

import java.util.Collections;

import static com.devicehive.configuration.Constants.ID;
import static com.devicehive.configuration.Constants.NAME;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NETWORKS_LISTED;
import static javax.ws.rs.core.Response.Status.*;

@Service
public class NetworkResourceImpl implements NetworkResource {

    private static final Logger logger = LoggerFactory.getLogger(NetworkResourceImpl.class);

    private final NetworkService networkService;

    @Autowired
    public NetworkResourceImpl(NetworkService networkService) {
        this.networkService = networkService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void list(String name, String namePattern, String sortField, String sortOrder, Integer take, Integer skip,
                     @Suspended final AsyncResponse asyncResponse) {

        logger.debug("Network list requested");

        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !NAME.equalsIgnoreCase(sortField)) {
            logger.error("Unable to proceed network list request. Invalid sortField");
            final Response response = ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
            asyncResponse.resume(response);
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!principal.areAllNetworksAvailable() && (principal.getNetworkIds() == null || principal.getNetworkIds().isEmpty())) {
            logger.warn("Unable to get list for empty networks");
            final Response response = ResponseFactory.response(OK, Collections.<NetworkVO>emptyList(), NETWORKS_LISTED);
            asyncResponse.resume(response);
        } else {
            networkService.list(name, namePattern, sortField, sortOrder, take, skip, principal)
                    .thenApply(networks -> {
                        logger.debug("Network list request proceed successfully.");
                        return ResponseFactory.response(OK, networks, NETWORKS_LISTED);
                    }).thenAccept(asyncResponse::resume);
        }
    }

    @Override
    public void count(String name, String namePattern, AsyncResponse asyncResponse) {
        logger.debug("Network count requested");
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        networkService.count(name, namePattern, principal)
                .thenApply(count -> {
                    logger.debug("Network count request proceed successfully");
                    return ResponseFactory.response(OK, count, JsonPolicyDef.Policy.NETWORKS_LISTED);
                }).thenApply(asyncResponse::resume);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response get(long id) {
        logger.debug("Network get requested.");
        
        NetworkWithUsersAndDevicesVO existing = networkService.getWithDevices(id);
        if (existing == null) {
            logger.error("Network with id =  {} does not exists", id);
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.NETWORK_NOT_FOUND, id)));
        }
        return ResponseFactory.response(OK, existing, JsonPolicyDef.Policy.NETWORK_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response insert(NetworkUpdate network) {
        logger.debug("Network insert requested");
        NetworkVO result = networkService.create(network.convertTo());
        logger.debug("New network has been created");
        return ResponseFactory.response(CREATED, result, JsonPolicyDef.Policy.NETWORK_SUBMITTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response update(NetworkUpdate networkToUpdate, long id) {
        logger.debug("Network update requested. Id : {}", id);
        networkService.update(id, networkToUpdate);
        logger.debug("Network has been updated successfully. Id : {}", id);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response delete(long id, boolean force) {
        logger.debug("Network delete requested");
        boolean isDeleted = networkService.delete(id, force);
        if (!isDeleted) {
            logger.error(String.format(Messages.NETWORK_NOT_FOUND, id));
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.NETWORK_NOT_FOUND, id)));
        }
        logger.debug("Network with id = {} does not exists any more.", id);
        return ResponseFactory.response(NO_CONTENT);
    }
}
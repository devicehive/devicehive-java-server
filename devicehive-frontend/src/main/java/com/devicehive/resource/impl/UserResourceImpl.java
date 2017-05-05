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
import static com.devicehive.auth.HiveAction.MANAGE_USER;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.response.UserNetworkResponse;
import com.devicehive.model.response.UserResponse;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.resource.UserResource;
import com.devicehive.resource.converters.SortOrderQueryParamParser;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.UserService;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;
import com.devicehive.vo.UserWithNetworkVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.Objects;

import static com.devicehive.configuration.Constants.ID;
import static com.devicehive.configuration.Constants.LOGIN;
import static com.devicehive.configuration.Constants.USER_ANONYMOUS_CREATION;
import com.devicehive.service.configuration.ConfigurationService;
import static javax.ws.rs.core.Response.Status.*;

@Service
public class UserResourceImpl implements UserResource {

    private static final Logger logger = LoggerFactory.getLogger(UserResourceImpl.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void list(String login, String loginPattern, Integer role, Integer status, String sortField,
            String sortOrderSt, Integer take, Integer skip, @Suspended final AsyncResponse asyncResponse) {

        final boolean sortOrder = SortOrderQueryParamParser.parse(sortOrderSt);

        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !LOGIN.equalsIgnoreCase(sortField)) {
            final Response response = ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
            asyncResponse.resume(response);
        } else {
            if (sortField != null) {
                sortField = sortField.toLowerCase();
            }

            userService.list(login, loginPattern, role, status, sortField, sortOrder, take, skip)
                    .thenApply(users -> {
                        logger.debug("User list request proceed successfully");

                        return ResponseFactory.response(OK, users, JsonPolicyDef.Policy.USERS_LISTED);
                    }).thenAccept(asyncResponse::resume);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getUser(Long userId) {
        UserVO currentLoggedInUser = findCurrentUserFromAuthContext();

        UserWithNetworkVO fetchedUser = null;

        if (currentLoggedInUser != null && currentLoggedInUser.getRole() == UserRole.ADMIN) {
            fetchedUser = userService.findUserWithNetworks(userId);
        } else if (currentLoggedInUser != null && currentLoggedInUser.getRole() == UserRole.CLIENT && Objects.equals(currentLoggedInUser.getId(), userId)) {
            fetchedUser = userService.findUserWithNetworks(currentLoggedInUser.getId());
        } else {
            return ResponseFactory.response(FORBIDDEN, new ErrorResponse(NOT_FOUND.getStatusCode(), Messages.USER_NOT_FOUND));
        }

        if (fetchedUser == null) {
            logger.error("Can't get user with id {}: user not found", userId);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(), Messages.USER_NOT_FOUND));
        }

        return ResponseFactory.response(OK, UserResponse.createFromUser(fetchedUser), JsonPolicyDef.Policy.USER_PUBLISHED);
    }

    @Override
    public Response getCurrent() {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long id = principal.getUser().getId();
        UserVO currentUser = userService.findUserWithNetworks(id);

        if (currentUser == null) {
            return ResponseFactory.response(CONFLICT, new ErrorResponse(CONFLICT.getStatusCode(), Messages.CAN_NOT_GET_CURRENT_USER));
        }

        return ResponseFactory.response(OK, currentUser, JsonPolicyDef.Policy.USER_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response insertUser(UserUpdate userToCreate) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Boolean isAnonymousCreateAllowed = configurationService.getBoolean(USER_ANONYMOUS_CREATION, false);
        Boolean isAuthenticatedAndHasPermission = (principal.isAuthenticated() && principal.getActions() != null && 
                principal.getActions().contains(MANAGE_USER));
        Boolean isCreateAllowed = isAnonymousCreateAllowed || isAuthenticatedAndHasPermission;
        if (isCreateAllowed) {
            String password = !userToCreate.getPassword().isPresent() ? null : userToCreate.getPassword().orElse(null);
            if (isAuthenticatedAndHasPermission) {
                UserVO created = userService.createUser(userToCreate.convertTo(), password);
                return ResponseFactory.response(CREATED, created, JsonPolicyDef.Policy.USER_SUBMITTED);
            } else {
                UserWithNetworkVO created = userService.createUserWithNetwork(userToCreate.convertTo(), password);
                return ResponseFactory.response(CREATED, created, JsonPolicyDef.Policy.USER_PUBLISHED);
            }
        } else {
            return ResponseFactory.response(UNAUTHORIZED, new ErrorResponse(UNAUTHORIZED.getStatusCode(), Messages.FORBIDDEN_INSERT_USER));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response updateUser(UserUpdate user, Long userId) {
        userService.updateUser(userId, user, UserRole.ADMIN);
        return ResponseFactory.response(NO_CONTENT);
    }

    @Override
    public Response updateCurrentUser(UserUpdate user) {
        UserVO curUser = findCurrentUserFromAuthContext();
        userService.updateUser(curUser.getId(), user, curUser.getRole());
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response deleteUser(long userId) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserVO currentUser = null;
        if (principal.getUser() != null) {
            currentUser = principal.getUser();
        }

        if (currentUser != null && currentUser.getId().equals(userId)) {
            logger.debug("Rejected removing current user");
            throw new HiveException(Messages.CANT_DELETE_CURRENT_USER_KEY, FORBIDDEN.getStatusCode());
        }
        userService.deleteUser(userId);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getNetwork(long id, long networkId) {
        UserWithNetworkVO existingUser = userService.findUserWithNetworks(id);
        if (existingUser == null) {
            logger.error("Can't get network with id {}: user {} not found", networkId, id);
            throw new HiveException(Messages.USER_NOT_FOUND, NOT_FOUND.getStatusCode());
        }
        for (NetworkVO network : existingUser.getNetworks()) {
            if (network.getId() == networkId) {
                return ResponseFactory.response(OK, UserNetworkResponse.fromNetwork(network), JsonPolicyDef.Policy.NETWORKS_LISTED);
            }
        }
        throw new NotFoundException(String.format(Messages.USER_NETWORK_NOT_FOUND, networkId, id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response assignNetwork(long id, long networkId) {
        userService.assignNetwork(id, networkId);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response unassignNetwork(long id, long networkId) {
        userService.unassignNetwork(id, networkId);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * Finds current user from authentication context, handling key and basic
     * authorisation schemes.
     *
     * @return user object or null
     */
    private UserVO findCurrentUserFromAuthContext() {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.getUser();
    }

}

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
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.response.UserDeviceTypeResponse;
import com.devicehive.model.response.UserNetworkResponse;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.resource.UserResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.BaseDeviceTypeService;
import com.devicehive.service.DeviceTypeService;
import com.devicehive.service.UserService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Objects;

import static com.devicehive.configuration.Constants.ID;
import static com.devicehive.configuration.Constants.LOGIN;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_TYPES_LISTED;
import static javax.ws.rs.core.Response.Status.*;

@Service
public class UserResourceImpl implements UserResource {

    private static final Logger logger = LoggerFactory.getLogger(UserResourceImpl.class);

    private final UserService userService;
    private final DeviceTypeService deviceTypeService;
    private final HiveValidator hiveValidator;

    @Autowired
    public UserResourceImpl(UserService userService, DeviceTypeService deviceTypeService, HiveValidator hiveValidator) {
        this.userService = userService;
        this.deviceTypeService = deviceTypeService;
        this.hiveValidator = hiveValidator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void list(String login, String loginPattern, Integer role, Integer status, String sortField,
            String sortOrder, Integer take, Integer skip, @Suspended final AsyncResponse asyncResponse) {

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
    public void count(String login, String loginPattern, Integer role, Integer status, AsyncResponse asyncResponse) {
        logger.debug("User count requested");

        userService.count(login, loginPattern, role, status)
                .thenApply(count -> {
                    logger.debug("User count request proceed successfully");
                    return ResponseFactory.response(OK, count, JsonPolicyDef.Policy.USERS_LISTED);
                }).thenAccept(asyncResponse::resume);
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
            return ResponseFactory.response(FORBIDDEN,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.USER_NOT_FOUND, userId)));
        }

        if (fetchedUser == null) {
            logger.error("Can't get user with id {}: user not found", userId);
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.USER_NOT_FOUND, userId)));
        }

        return ResponseFactory.response(OK, fetchedUser, JsonPolicyDef.Policy.USER_PUBLISHED);
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
        hiveValidator.validate(userToCreate);
        String password = userToCreate.getPassword().orElse(null);
        UserVO created = userService.createUser(userToCreate.convertTo(), password);
        return ResponseFactory.response(CREATED, created, JsonPolicyDef.Policy.USER_SUBMITTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response updateUser(UserUpdate user, Long userId) {
        UserVO curUser = findCurrentUserFromAuthContext();
        userService.updateUser(userId, user, curUser);
        return ResponseFactory.response(NO_CONTENT);
    }

    @Override
    public Response updateCurrentUser(UserUpdate user) {
        UserVO curUser = findCurrentUserFromAuthContext();
        userService.updateUser(curUser.getId(), user, curUser);
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
            ErrorResponse errorResponseEntity = new ErrorResponse(FORBIDDEN.getStatusCode(),
                    Messages.CANT_DELETE_CURRENT_USER_KEY);
            return ResponseFactory.response(FORBIDDEN, errorResponseEntity);
        }
        boolean isDeleted = userService.deleteUser(userId);
        if (!isDeleted) {
            logger.error(String.format(Messages.USER_NOT_FOUND, userId));
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.USER_NOT_FOUND, userId)));
        }
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
            ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.USER_NOT_FOUND, id));
            return ResponseFactory.response(NOT_FOUND, errorResponseEntity);
        }
        for (NetworkVO network : existingUser.getNetworks()) {
            if (network.getId() == networkId) {
                return ResponseFactory.response(OK, UserNetworkResponse.fromNetwork(network), JsonPolicyDef.Policy.NETWORKS_LISTED);
            }
        }
        ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                String.format(Messages.USER_NETWORK_NOT_FOUND, networkId, id));
        return ResponseFactory.response(NOT_FOUND, errorResponseEntity);
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
     * {@inheritDoc}
     */
    @Override
    public Response getDeviceType(long id, long deviceTypeId) {
        UserWithDeviceTypeVO existingUser = userService.findUserWithDeviceType(id);
        if (existingUser == null) {
            logger.error("Can't get device type with id {}: user {} not found", deviceTypeId, id);
            ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.USER_NOT_FOUND, id));
            return ResponseFactory.response(NOT_FOUND, errorResponseEntity);
        }

        if (existingUser.getAllDeviceTypesAvailable()) {
            DeviceTypeVO deviceTypeVO = deviceTypeService.getWithDevices(deviceTypeId);
            if (deviceTypeVO != null) {
                return ResponseFactory.response(OK, UserDeviceTypeResponse.fromDeviceType(deviceTypeVO), JsonPolicyDef.Policy.DEVICE_TYPES_LISTED);
            }
        }

        for (DeviceTypeVO deviceType : existingUser.getDeviceTypes()) {
            if (deviceType.getId() == deviceTypeId) {
                return ResponseFactory.response(OK, UserDeviceTypeResponse.fromDeviceType(deviceType), JsonPolicyDef.Policy.DEVICE_TYPES_LISTED);
            }
        }
        ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                String.format(Messages.USER_DEVICE_TYPE_NOT_FOUND, deviceTypeId, id));
        return ResponseFactory.response(NOT_FOUND, errorResponseEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getDeviceTypes(long id, @Suspended final AsyncResponse asyncResponse) {
        UserWithDeviceTypeVO existingUser = userService.findUserWithDeviceType(id);
        if (existingUser == null) {
            logger.error("Can't get device types for user with id {}: user not found", id);
            ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.USER_NOT_FOUND, id));
            asyncResponse.resume(ResponseFactory.response(NOT_FOUND, errorResponseEntity));
        } else {
            if (existingUser.getAllDeviceTypesAvailable()) {
                deviceTypeService.listAll().thenApply(deviceTypeVOS -> {
                    logger.debug("User list request proceed successfully");
                    return ResponseFactory.response(OK, deviceTypeVOS, JsonPolicyDef.Policy.DEVICE_TYPES_LISTED);
                }).thenAccept(asyncResponse::resume);
            } else if (!existingUser.getAllDeviceTypesAvailable() && (existingUser.getDeviceTypes() == null || existingUser.getDeviceTypes().isEmpty())) {
                logger.warn("Unable to get list for empty device types");
                asyncResponse.resume(ResponseFactory.response(OK, Collections.<DeviceTypeVO>emptyList(), DEVICE_TYPES_LISTED));
            } else {
                asyncResponse.resume(ResponseFactory.response(OK, existingUser.getDeviceTypes(), JsonPolicyDef.Policy.DEVICE_TYPES_LISTED));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response assignDeviceType(long id, long deviceTypeId) {
        userService.assignDeviceType(id, deviceTypeId);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response unassignDeviceType(long id, long deviceTypeId) {
        userService.unassignDeviceType(id, deviceTypeId);
        return ResponseFactory.response(NO_CONTENT);
    }

    @Override
    public Response allowAllDeviceTypes(long id) {
        userService.allowAllDeviceTypes(id);
        return ResponseFactory.response(NO_CONTENT);
    }

    @Override
    public Response disallowAllDeviceTypes(long id) {
        userService.disallowAllDeviceTypes(id);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * Finds current user from authentication context, handling token
     * authorisation schemes.
     *
     * @return user object or null
     */
    private UserVO findCurrentUserFromAuthContext() {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.getUser();
    }

}

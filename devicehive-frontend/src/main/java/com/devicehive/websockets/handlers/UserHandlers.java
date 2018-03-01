package com.devicehive.websockets.handlers;

/*
 * #%L
 * DeviceHive Frontend Logic
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
import com.devicehive.auth.websockets.HiveWebsocketAuth;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.handler.WebSocketClientHandler;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.response.UserDeviceTypeResponse;
import com.devicehive.model.response.UserNetworkResponse;
import com.devicehive.model.rpc.CountUserRequest;
import com.devicehive.model.rpc.ListUserRequest;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.service.BaseDeviceTypeService;
import com.devicehive.service.DeviceTypeService;
import com.devicehive.service.UserService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.*;
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
import java.util.Objects;
import java.util.Optional;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static com.devicehive.model.rpc.ListUserRequest.createListUserRequest;
import static javax.ws.rs.core.Response.Status.*;

@Component
public class UserHandlers {

    private static final Logger logger = LoggerFactory.getLogger(UserHandlers.class);

    private final UserService userService;
    private final DeviceTypeService deviceTypeService;
    private final HiveValidator hiveValidator;
    private final WebSocketClientHandler clientHandler;
    private final Gson gson;

    @Autowired
    public UserHandlers(UserService userService,
                        DeviceTypeService deviceTypeService,
                        HiveValidator hiveValidator,
                        WebSocketClientHandler clientHandler,
                        Gson gson) {
        this.userService = userService;
        this.deviceTypeService = deviceTypeService;
        this.hiveValidator = hiveValidator;
        this.clientHandler = clientHandler;
        this.gson = gson;
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_USER')")
    public void processUserList(JsonObject request, WebSocketSession session) {
        ListUserRequest listUserRequest = createListUserRequest(request);

        String sortField = Optional.ofNullable(listUserRequest.getSortField()).map(String::toLowerCase).orElse(null);
        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !LOGIN.equalsIgnoreCase(sortField)) {
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, BAD_REQUEST.getStatusCode());
        }
        
        userService.list(listUserRequest)
                .thenAccept(users -> {
                    logger.debug("User list request proceed successfully");
                    WebSocketResponse response = new WebSocketResponse();
                    response.addValue(USERS, users, USERS_LISTED);
                    clientHandler.sendMessage(request, response, session);
                });
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_USER')")
    public void processUserCount(JsonObject request, WebSocketSession session) {
        CountUserRequest countUserRequest = CountUserRequest.createCountUserRequest(request);

        WebSocketResponse response = new WebSocketResponse();
        userService.count(countUserRequest)
                .thenAccept(count -> {
                    logger.debug("User count request proceed successfully");
                    response.addValue(COUNT, count.getCount(), null);
                    clientHandler.sendMessage(request, response, session);
                });
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_USER')")
    public void processUserGet(JsonObject request, WebSocketSession session) {
        Long userId = gson.fromJson(request.get(USER_ID), Long.class);
        if (userId == null) {
            logger.error(Messages.USER_ID_REQUIRED);
            throw new HiveException(Messages.USER_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        
        UserVO currentLoggedInUser = findCurrentUserFromAuthContext();

        UserWithNetworkVO fetchedUser;
        if (currentLoggedInUser != null && currentLoggedInUser.getRole() == UserRole.ADMIN) {
            fetchedUser = userService.findUserWithNetworks(userId);
        } else if (currentLoggedInUser != null 
                && currentLoggedInUser.getRole() == UserRole.CLIENT 
                && Objects.equals(currentLoggedInUser.getId(), userId)) {
            fetchedUser = userService.findUserWithNetworks(currentLoggedInUser.getId());
        } else {
            clientHandler.sendErrorResponse(request, NOT_FOUND.getStatusCode(),
                    String.format(Messages.USER_NOT_FOUND, userId), session);
            return;
        }

        if (fetchedUser == null) {
            logger.error("Can't get user with id {}: user not found", userId);
            clientHandler.sendErrorResponse(request, NOT_FOUND.getStatusCode(),
                    String.format(Messages.USER_NOT_FOUND, userId), session);
        } else {
            WebSocketResponse response = new WebSocketResponse();
            response.addValue(USER, fetchedUser, USER_PUBLISHED);
            clientHandler.sendMessage(request, response, session);
        }
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_CURRENT_USER')")
    public void processUserGetCurrent(JsonObject request, WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long id = principal.getUser().getId();
        UserVO currentUser = userService.findUserWithNetworks(id);


        if (currentUser == null) {
            clientHandler.sendErrorResponse(request, CONFLICT.getStatusCode(), Messages.CAN_NOT_GET_CURRENT_USER, session);
        } else {
            WebSocketResponse response = new WebSocketResponse();
            response.addValue(CURRENT_USER, currentUser, USER_PUBLISHED);
            clientHandler.sendMessage(request, response, session);
        }
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_USER')")
    public void processUserInsert(JsonObject request, WebSocketSession session) {
        UserUpdate userToCreate = gson.fromJson(request.get(USER), UserUpdate.class);
        if (userToCreate == null) {
            logger.error(Messages.USER_REQUIRED);
            throw new HiveException(Messages.USER_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        
        hiveValidator.validate(userToCreate);
        String password = userToCreate.getPassword().orElse(null);
        UserVO created = userService.createUser(userToCreate.convertTo(), password);
        
        WebSocketResponse response = new WebSocketResponse();
        response.addValue(USER, created, USER_SUBMITTED);
        
        clientHandler.sendMessage(request, response, session);
    }

    /**
     * {@inheritDoc}
     */
    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_USER')")
    public void processUserUpdate(JsonObject request, WebSocketSession session) {
        UserUpdate user = gson.fromJson(request.get(USER), UserUpdate.class);
        if (user == null) {
            logger.error(Messages.USER_REQUIRED);
            throw new HiveException(Messages.USER_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        
        Long userId = gson.fromJson(request.get(USER_ID), Long.class);
        if (userId == null) {
            logger.error(Messages.USER_ID_REQUIRED);
            throw new HiveException(Messages.USER_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        
        UserVO curUser = userService.findById(userId);
        if (curUser == null) {
            logger.error(Messages.USER_NOT_FOUND);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        
        userService.updateUser(userId, user, findCurrentUserFromAuthContext());
        clientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'UPDATE_CURRENT_USER')")
    public void processUserUpdateCurrent(JsonObject request, WebSocketSession session) {
        UserUpdate user = gson.fromJson(request.get(USER), UserUpdate.class);
        if (user == null) {
            logger.error(Messages.USER_REQUIRED);
            throw new HiveException(Messages.USER_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        
        UserVO curUser = findCurrentUserFromAuthContext();
        userService.updateUser(curUser.getId(), user, curUser);
        
        clientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_USER')")
    public void processUserDelete(JsonObject request, WebSocketSession session) {
        Long userId = gson.fromJson(request.get(USER_ID), Long.class);
        if (userId == null) {
            logger.error(Messages.USER_ID_REQUIRED);
            throw new HiveException(Messages.USER_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserVO currentUser = null;
        if (principal.getUser() != null) {
            currentUser = principal.getUser();
        }

        if (currentUser != null && currentUser.getId().equals(userId)) {
            logger.error("Rejected removing current user");
            throw new HiveException(Messages.CANT_DELETE_CURRENT_USER_KEY, FORBIDDEN.getStatusCode());
        }
        boolean isDeleted = userService.deleteUser(userId);
        if (!isDeleted) {
            logger.error(String.format(Messages.USER_NOT_FOUND, userId));
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        clientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_NETWORK')")
    public void processUserGetNetwork(JsonObject request, WebSocketSession session) {
        Long userId = gson.fromJson(request.get(USER_ID), Long.class);
        if (userId == null) {
            logger.error(Messages.USER_ID_REQUIRED);
            throw new HiveException(Messages.USER_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        Long networkId = gson.fromJson(request.get(NETWORK_ID), Long.class);
        if (networkId == null) {
            logger.error(Messages.NETWORK_ID_REQUIRED);
            throw new HiveException(Messages.NETWORK_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        UserWithNetworkVO existingUser = userService.findUserWithNetworks(userId);
        if (existingUser == null) {
            logger.error("Can't get network with id {}: user {} not found", networkId, userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        for (NetworkVO network : existingUser.getNetworks()) {
            if (networkId.equals(network.getId())) {
                WebSocketResponse response = new WebSocketResponse();
                response.addValue(NETWORK, UserNetworkResponse.fromNetwork(network), NETWORKS_LISTED);
                clientHandler.sendMessage(request, response, session);
                return;
            }
        }

        clientHandler.sendErrorResponse(request, NOT_FOUND.getStatusCode(),
                String.format(Messages.USER_NETWORK_NOT_FOUND, networkId, userId), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_NETWORK')")
    public void processUserAssignNetwork(JsonObject request, WebSocketSession session) {
        Long userId = gson.fromJson(request.get(USER_ID), Long.class);
        if (userId == null) {
            logger.error(Messages.USER_ID_REQUIRED);
            throw new HiveException(Messages.USER_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        
        Long networkId = gson.fromJson(request.get(NETWORK_ID), Long.class);
        if (networkId == null) {
            logger.error(Messages.NETWORK_ID_REQUIRED);
            throw new HiveException(Messages.NETWORK_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        
        userService.assignNetwork(userId, networkId);
        clientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_NETWORK')")
    public void processUserUnassignNetwork(JsonObject request, WebSocketSession session) {
        Long userId = gson.fromJson(request.get(USER_ID), Long.class);
        if (userId == null) {
            logger.error(Messages.USER_ID_REQUIRED);
            throw new HiveException(Messages.USER_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        
        Long networkId = gson.fromJson(request.get(NETWORK_ID), Long.class);
        if (networkId == null) {
            logger.error(Messages.NETWORK_ID_REQUIRED);
            throw new HiveException(Messages.NETWORK_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        userService.unassignNetwork(userId, networkId);
        clientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_TYPE')")
    public void processUserGetDeviceType(JsonObject request, WebSocketSession session) {
        Long userId = gson.fromJson(request.get(USER_ID), Long.class);
        if (userId == null) {
            logger.error(Messages.USER_ID_REQUIRED);
            throw new HiveException(Messages.USER_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        Long deviceTypeId = gson.fromJson(request.get(DEVICE_TYPE_ID), Long.class);
        if (deviceTypeId == null) {
            logger.error(Messages.DEVICE_TYPE_ID_REQUIRED);
            throw new HiveException(Messages.DEVICE_TYPE_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        UserWithDeviceTypeVO existingUser = userService.findUserWithDeviceType(userId);
        if (existingUser == null) {
            logger.error("Can't get device type with id {}: user {} not found", deviceTypeId, userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }

        for (DeviceTypeVO deviceType : existingUser.getDeviceTypes()) {
            if (deviceTypeId.equals(deviceType.getId())) {
                WebSocketResponse response = new WebSocketResponse();
                response.addValue(DEVICE_TYPE, UserDeviceTypeResponse.fromDeviceType(deviceType), DEVICE_TYPES_LISTED);
                clientHandler.sendMessage(request, response, session);
                return;
            }
        }

        clientHandler.sendErrorResponse(request, NOT_FOUND.getStatusCode(),
                String.format(Messages.USER_DEVICE_TYPE_NOT_FOUND, deviceTypeId, userId), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_DEVICE_TYPE')")
    public void processUserGetDeviceTypes(JsonObject request, WebSocketSession session) {
        Long userId = gson.fromJson(request.get(USER_ID), Long.class);
        if (userId == null) {
            logger.error(Messages.USER_ID_REQUIRED);
            throw new HiveException(Messages.USER_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        final UserWithDeviceTypeVO existingUser = userService.findUserWithDeviceType(userId);
        if (existingUser == null) {
            logger.error("Can't get device types for user with id {}: user not found", userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }

        final WebSocketResponse response = new WebSocketResponse();
        if (existingUser.getAllDeviceTypesAvailable()) {
            deviceTypeService.listAll().thenAccept(dt -> {
                logger.debug("Device types list request proceed successfully");
                response.addValue(DEVICE_TYPES, dt, DEVICE_TYPES_LISTED);
                clientHandler.sendMessage(request, response, session);
            });
        } else {
            if (!existingUser.getAllDeviceTypesAvailable() && (existingUser.getDeviceTypes() == null || existingUser.getDeviceTypes().isEmpty())) {
                logger.warn("Unable to get list for empty device types");
                response.addValue(DEVICE_TYPES, Collections.emptyList(), DEVICE_TYPES_LISTED);
            } else {
                response.addValue(DEVICE_TYPES, existingUser.getDeviceTypes(), DEVICE_TYPES_LISTED);
            }
            clientHandler.sendMessage(request, response, session);
        }
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_DEVICE_TYPE')")
    public void processUserAssignDeviceType(JsonObject request, WebSocketSession session) {
        Long userId = gson.fromJson(request.get(USER_ID), Long.class);
        if (userId == null) {
            logger.error(Messages.USER_ID_REQUIRED);
            throw new HiveException(Messages.USER_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        Long deviceTypeId = gson.fromJson(request.get(DEVICE_TYPE_ID), Long.class);
        if (deviceTypeId == null) {
            logger.error(Messages.DEVICE_TYPE_ID_REQUIRED);
            throw new HiveException(Messages.DEVICE_TYPE_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        userService.assignDeviceType(userId, deviceTypeId);
        clientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_DEVICE_TYPE')")
    public void processUserUnassignDeviceType(JsonObject request, WebSocketSession session) {
        Long userId = gson.fromJson(request.get(USER_ID), Long.class);
        if (userId == null) {
            logger.error(Messages.USER_ID_REQUIRED);
            throw new HiveException(Messages.USER_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        Long deviceTypeId = gson.fromJson(request.get(DEVICE_TYPE_ID), Long.class);
        if (deviceTypeId == null) {
            logger.error(Messages.DEVICE_TYPE_ID_REQUIRED);
            throw new HiveException(Messages.DEVICE_TYPE_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        userService.unassignDeviceType(userId, deviceTypeId);
        clientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_DEVICE_TYPE')")
    public void processUserAllowAllDeviceTypes(JsonObject request, WebSocketSession session) {
        Long userId = gson.fromJson(request.get(USER_ID), Long.class);
        if (userId == null) {
            logger.error(Messages.USER_ID_REQUIRED);
            throw new HiveException(Messages.USER_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        userService.allowAllDeviceTypes(userId);
        clientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_DEVICE_TYPE')")
    public void processUserDisallowAllDeviceTypes(JsonObject request, WebSocketSession session) {
        Long userId = gson.fromJson(request.get(USER_ID), Long.class);
        if (userId == null) {
            logger.error(Messages.USER_ID_REQUIRED);
            throw new HiveException(Messages.USER_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        userService.disallowAllDeviceTypes(userId);
        clientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    private UserVO findCurrentUserFromAuthContext() {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.getUser();
    }
}

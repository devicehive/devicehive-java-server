package com.devicehive.resource.impl;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.response.UserNetworkResponse;
import com.devicehive.model.response.UserResponse;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.resource.UserResource;
import com.devicehive.resource.converters.SortOrderQueryParamParser;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.devicehive.configuration.Constants.ID;
import static com.devicehive.configuration.Constants.LOGIN;
import static javax.ws.rs.core.Response.Status.*;

@Service
public class UserResourceImpl implements UserResource {
    private static final Logger logger = LoggerFactory.getLogger(UserResourceImpl.class);

    @Autowired
    private UserService userService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getUsersList(String login, String loginPattern, Integer role, Integer status, String sortField, String sortOrderSt, Integer take, Integer skip) {

        boolean sortOrder = SortOrderQueryParamParser.parse(sortOrderSt);

        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !LOGIN.equalsIgnoreCase(sortField)) {
            return ResponseFactory.response(BAD_REQUEST,
                                            new ErrorResponse(BAD_REQUEST.getStatusCode(),
                                                              Messages.INVALID_REQUEST_PARAMETERS));
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }

        List<User> result = userService.getList(login, loginPattern, role, status, sortField, sortOrder, take, skip);

        logger.debug("User list request proceed successfully. Login = {}, loginPattern = {}, role = {}, status = {}, " +
                        "sortField = {}, " +
                        "sortOrder = {}, take = {}, skip = {}", login, loginPattern, role, status, sortField, sortOrder,
                take, skip);

        return ResponseFactory.response(OK, result, JsonPolicyDef.Policy.USERS_LISTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getUser(Long userId) {

        User user = userService.findUserWithNetworks(userId);

        if (user == null) {
            logger.error("Can't get user with id {}: user not found", userId);
            return ResponseFactory.response(NOT_FOUND,
                                            new ErrorResponse(NOT_FOUND.getStatusCode(),
                                                              Messages.USER_NOT_FOUND));
        }

        return ResponseFactory.response(OK,
                                        UserResponse.createFromUser(user),
                                        JsonPolicyDef.Policy.USER_PUBLISHED);
    }

    @Override
    public Response getCurrent() {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long id = principal.getUser() != null ? principal.getUser().getId() : principal.getKey().getUser().getId();
        User currentUser = userService.findUserWithNetworks(id);

        if (currentUser == null) {
            return ResponseFactory.response(CONFLICT,
                                            new ErrorResponse(CONFLICT.getStatusCode(),
                                                              Messages.CAN_NOT_GET_CURRENT_USER));
        }

        return ResponseFactory.response(OK, currentUser, JsonPolicyDef.Policy.USER_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response insertUser(UserUpdate userToCreate) {
        String password = userToCreate.getPassword() == null ? null : userToCreate.getPassword().getValue();
        User created = userService.createUser(userToCreate.convertTo(), password);
        return ResponseFactory.response(CREATED, created, JsonPolicyDef.Policy.USER_SUBMITTED);
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
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User curUser = principal.getUser() != null ? principal.getUser() : principal.getKey().getUser();
        userService.updateUser(curUser.getId(), user, curUser.getRole());
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response deleteUser(long userId) {
        userService.deleteUser(userId);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getNetwork(long id, long networkId) {
        User existingUser = userService.findUserWithNetworks(id);
        if (existingUser == null) {
            logger.error("Can't get network with id {}: user {} not found", networkId, id);
            throw new HiveException(Messages.USER_NOT_FOUND, NOT_FOUND.getStatusCode());
        }
        for (Network network : existingUser.getNetworks()) {
            if (network.getId() == networkId) {
                return ResponseFactory.response(OK,
                                                UserNetworkResponse.fromNetwork(network),
                                                JsonPolicyDef.Policy.NETWORKS_LISTED);
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

}

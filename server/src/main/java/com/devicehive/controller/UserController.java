package com.devicehive.controller;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.controller.converters.SortOrder;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.response.UserNetworkResponse;
import com.devicehive.model.response.UserResponse;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.service.UserService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.Response.Status.*;

@Path("/user")
@LogExecutionTime
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private UserService userService;

    @EJB
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /**
     * This method will generate following output
     * <p/>
     * <code>
     * [
     * {
     * "id": 2,
     * "login": "login",
     * "role": 0,
     * "status": 0,
     * "lastLogin": "1970-01-01 03:00:00.0"
     * },
     * {
     * "id": 3,
     * "login": "login1",
     * "role": 1,
     * "status": 2,
     * "lastLogin": "1970-01-01 03:00:00.0"
     * }
     * ]
     * </code>
     *
     * @param login        user login ignored, when loginPattern is specified
     * @param loginPattern login pattern (LIKE %VALUE%) user login will be ignored, if not null
     * @param role         User's role ADMIN - 0, CLIENT - 1
     * @param status       ACTIVE - 0 (normal state, user can logon) , LOCKED_OUT - 1 (locked for multiple login failures), DISABLED - 2 , DELETED - 3;
     * @param sortField    either of "login", "loginAttempts", "role", "status", "lastLogin"
     * @param sortOrder    either ASC or DESC
     * @param take         like SQL LIMIT
     * @param skip         like SQL OFFSET
     * @return List of User
     */
    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    public Response getUsersList(@QueryParam("login") String login,
                                 @QueryParam("loginPattern") String loginPattern,
                                 @QueryParam("role") Integer role,
                                 @QueryParam("status") Integer status,
                                 @QueryParam("sortField") String sortField,
                                 @QueryParam("sortOrder") @SortOrder Boolean sortOrder,
                                 @QueryParam("take") Integer take,
                                 @QueryParam("skip") Integer skip) {
        logger.debug("User list requested. Login = {}, loginPattern = {}, role = {}, status = {}, sortField = {}, " +
                "sortOrder = {}, take = {}, skip = {}", login, loginPattern, role, status, sortField, sortOrder,
                take, skip);

        if (sortOrder == null) {
            sortOrder = true;
        }

        if (!"ID".equalsIgnoreCase(sortField) && !"Login".equalsIgnoreCase(sortField) && sortField != null) {
            return ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(), ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
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
     * Method will generate following output:
     * <p/>
     * <code>
     * {
     * "id": 2,
     * "login": "login",
     * "status": 0,
     * "networks": [
     * {
     * "network": {
     * "id": 5,
     * "key": "network key",
     * "name": "name of network",
     * "description": "short description of network"
     * }
     * }
     * ],
     * "lastLogin": "1970-01-01 03:00:00.0"
     * }
     * </code>
     * <p/>
     * If success, response with status 200, if user is not found 400
     *
     * @param userId user id
     */
    @GET
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response getUser(@PathParam("id") Long userId) {

        User user = userService.findUserWithNetworks(userId);

        if (user == null) {
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                    "User not found."));
        }

        return ResponseFactory.response(OK,
                UserResponse.createFromUser(user),
                JsonPolicyDef.Policy.USER_PUBLISHED);
    }

    @GET
    @Path("/current")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response getCurrent() {
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        Long id = principal.getUser().getId();
        User currentUser = userService.findUserWithNetworks(id);

        if (currentUser == null) {
            return ResponseFactory
                    .response(CONFLICT, new ErrorResponse(CONFLICT.getStatusCode(), "Could not get current user."));
        }

        return ResponseFactory.response(OK, currentUser, JsonPolicyDef.Policy.USER_PUBLISHED);
    }

    /**
     * One needs to provide user resource in request body (all parameters are mandatory):
     * <p/>
     * <code>
     * {
     * "login":"login"
     * "role":0
     * "status":0
     * "password":"qwerty"
     * }
     * </code>
     * <p/>
     * In case of success server will provide following response with code 201
     * <p/>
     * <code>
     * {
     * "id": 1,
     * "lastLogin": null
     * }
     * </code>
     *
     * @return Empty body, status 201 if success, 403 if forbidden, 400 otherwise
     */
    @POST
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @JsonPolicyDef(JsonPolicyDef.Policy.USERS_LISTED)
    public Response insertUser(UserUpdate userToCreate) {
        String password = userToCreate.getPassword() == null ? null : userToCreate.getPassword().getValue();
        User created = userService.createUser(userToCreate.convertTo(), password);
        return ResponseFactory.response(CREATED, created, JsonPolicyDef.Policy.USER_SUBMITTED);
    }

    /**
     * Updates user. One should specify following json to update user (none of parameters are mandatory, bot neither of them can be null):
     * <p/>
     * <code>
     * {
     * "login": "login",
     * "role": 0,
     * "status": 0,
     * "password": "password"
     * }
     * </code>
     * <p/>
     * role:  Administrator - 0, Client - 1
     * status: ACTIVE - 0 (normal state, user can logon) , LOCKED_OUT - 1 (locked for multiple login failures), DISABLED - 2 , DELETED - 3;
     *
     * @param userId - id of user being edited
     * @return empty response, status 201 if succeeded, 403 if action is forbidden, 400 otherwise
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(UserUpdate user, @PathParam("id") Long userId) {
        userService.updateUser(userId, user);
        return ResponseFactory.response(NO_CONTENT);
    }

    @PUT
    @Path("/current")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateCurrentUser(UserUpdate user) {
        Long id = ThreadLocalVariablesKeeper.getPrincipal().getUser().getId();
        userService.updateUser(id, user);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * Deletes user by id
     *
     * @param userId id of user to delete
     * @return empty response. state 204 in case of success, 404 if not found
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response deleteUser(@PathParam("id") long userId) {
        userService.deleteUser(userId);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * Method returns following body in case of success (status 200):
     * <code>
     * {
     * "id": 5,
     * "key": "network_key",
     * "name": "network name",
     * "description": "short description of net"
     * }
     * </code>
     * in case, there is no such network, or user, or user doesn't have access
     *
     * @param id        user id
     * @param networkId network id
     */
    @GET
    @Path("/{id}/network/{networkId}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response getNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {
        User existingUser = userService.findUserWithNetworks(id);
        if (existingUser == null) {
            throw new HiveException("User not found.", NOT_FOUND.getStatusCode());
        }
        for (Network network : existingUser.getNetworks()) {
            if (network.getId() == networkId) {
                return ResponseFactory.response(OK,
                        UserNetworkResponse.fromNetwork(network),
                        JsonPolicyDef.Policy.NETWORKS_LISTED);
            }
        }
        throw new NotFoundException("User network not found.");
    }

    /**
     * Request body must be empty. Returns Empty body.
     *
     * @param id        user id
     * @param networkId network id
     */
    @PUT
    @Path("/{id}/network/{networkId}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response assignNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {
        userService.assignNetwork(id, networkId);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * Removes user permissions on network
     *
     * @param id        user id
     * @param networkId network id
     * @return Empty body. Status 204 in case of success, 404 otherwise
     */
    @DELETE
    @Path("/{id}/network/{networkId}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response unassignNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {
        userService.unassignNetwork(id, networkId);
        return ResponseFactory.response(NO_CONTENT);
    }

}

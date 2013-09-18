package com.devicehive.controller;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.response.UserNetworkResponse;
import com.devicehive.model.response.UserResponse;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.service.UserService;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.utils.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("/user")
@LogExecutionTime
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    @EJB
    private UserService userService;

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
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        }

        List<User> result = userService.getList(login, loginPattern, role, status, sortField, sortOrder, take, skip);

        logger.debug("User list request proceed successfully. Login = {}, loginPattern = {}, role = {}, status = {}, " +
                "sortField = {}, " +
                "sortOrder = {}, take = {}, skip = {}", login, loginPattern, role, status, sortField, sortOrder,
                take, skip);

        return ResponseFactory.response(Response.Status.OK, result, JsonPolicyDef.Policy.USERS_LISTED);
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
    public Response getUser(@PathParam("id") String userId, @Context SecurityContext securityContext) {
        if (userId.equalsIgnoreCase(Constants.CURRENT_USER)) {
            return getCurrent(securityContext);
        }
        Long id;
        try {
            id = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new HiveException("Bad user identifier :" + userId, Response.Status.BAD_REQUEST.getStatusCode());
        }
        User user = userService.findUserWithNetworks(id);

        if (user == null) {
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("User not found."));
        }

        return ResponseFactory.response(Response.Status.OK,
                UserResponse.createFromUser(user),
                JsonPolicyDef.Policy.USER_PUBLISHED);
    }

    private Response getCurrent(SecurityContext securityContext) {
        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        Long id = principal.getUser().getId();
        User currentUser = userService.findUserWithNetworks(id);

        if (currentUser == null) {
            return ResponseFactory
                    .response(Response.Status.CONFLICT, new ErrorResponse("Could not get current user."));
        }

        return ResponseFactory.response(Response.Status.OK, currentUser, JsonPolicyDef.Policy.USER_PUBLISHED);
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
//
//        //neither we want left some params omitted
//        if (user.getLogin() == null || user.getPassword() == null || user.getRole() == null
//                || user.getStatus() == null) {
//            return ResponseFactory.response(Response.Status.BAD_REQUEST,
//                    new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
//        }
//        //nor we want these parameters to be null
//        if (user.getLogin().getValue() == null || user.getPassword().getValue() == null
//                || user.getRole().getValue() == null || user.getStatus().getValue() == null) {
//            return ResponseFactory.response(Response.Status.BAD_REQUEST,
//                    new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
//        }
//        User created = userService.createUser(user.getLogin().getValue(),
//                user.getRoleEnum(),
//                user.getStatusEnum(),
//                user.getPassword().getValue());
        String password = userToCreate.getPassword() == null ? null : userToCreate.getPassword().getValue();
        User created = userService.createUser(userToCreate.convertTo(), password);
        return ResponseFactory.response(Response.Status.CREATED, created, JsonPolicyDef.Policy.USER_SUBMITTED);
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
    public Response updateUser(UserUpdate user, @PathParam("id") String userId,@Context SecurityContext securityContext) {
        Long id;
        if (userId.equalsIgnoreCase(Constants.CURRENT_USER)) {
            HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
            id = principal.getUser().getId();
        } else {
            try {
                id = Long.parseLong(userId);
            } catch (NumberFormatException e) {
                throw new HiveException("Bad user identifier :" + userId, Response.Status.BAD_REQUEST.getStatusCode());
            }
        }
        userService.updateUser(id, user);
        return ResponseFactory.response(Response.Status.NO_CONTENT);
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
        return ResponseFactory.response(Response.Status.NO_CONTENT);
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
                return ResponseFactory.response(Response.Status.OK,
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
        return ResponseFactory.response(Response.Status.NO_CONTENT);
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
        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }

}

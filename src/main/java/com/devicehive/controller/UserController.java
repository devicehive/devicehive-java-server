package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.dao.UserDAO;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.request.UserRequest;
import com.devicehive.model.response.UserResponse;
import com.devicehive.service.UserService;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * TODO JavaDoc
 */
@Path("/user")
public class UserController {

    @Inject
    private UserService userService;
    @Inject
    private UserDAO userDAO;
    @Context
    private ContainerRequestContext requestContext;

    /**
     * This method will generate following output
     *
     * <code>
     *  [
     *  {
     *  "id": 2,
     *  "login": "login",
     *  "role": 0,
     *  "status": 0,
     *  "lastLogin": "1970-01-01 03:00:00.0"
     *  },
     *  {
     *  "id": 3,
     *  "login": "login1",
     *  "role": 1,
     *  "status": 2,
     *  "lastLogin": "1970-01-01 03:00:00.0"
     *  }
     *]
     *</code>
     *
     * @param login user login ignored, when loginPattern is specified
     * @param loginPattern login pattern (LIKE %VALUE%) user login will be ignored, if not null
     * @param role User's role ADMIN - 0, CLIENT - 1
     * @param status ACTIVE - 0 (normal state, user can logon) , LOCKED_OUT - 1 (locked for multiple login failures), DISABLED - 2 , DELETED - 3;
     * @param sortField    either of "login", "loginAttempts", "role", "status", "lastLogin"
     * @param sortOrder either ASC or DESC
     * @param take like SQL LIMIT
     * @param skip like SQL OFFSET
     * @return List of User
     */
    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.USERS_LISTED)
    public Response getUsersList(
            @QueryParam("login") String login,
            @QueryParam("loginPattern") String loginPattern,
            @QueryParam("role") Integer role,
            @QueryParam("status") Integer status,
            @QueryParam("sortField") String sortField,
            @QueryParam("sortOrder") String sortOrder,
            @QueryParam("take") Integer take,
            @QueryParam("skip") Integer skip
    ) {
        boolean sortOrderAsc = true;

        if (sortOrder != null && !sortOrder.equalsIgnoreCase("DESC") && !sortOrder.equalsIgnoreCase("ASC")) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if ("DESC".equalsIgnoreCase(sortOrder)) {
            sortOrderAsc = false;
        }

        if (!"ID".equalsIgnoreCase(sortField) && !"Login".equalsIgnoreCase(sortField) && sortField != null) {  //ID??
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        //TODO validation for role and status
        List<User> result = userDAO.getList(login, loginPattern, role, status, sortField, sortOrderAsc, take, skip);

        return Response.ok().entity(result).build();
    }


    /**
     * Method will generate following output:
     *
     *<code>
     *{
     *     "id": 2,
     *     "login": "login",
     *     "status": 0,
     *     "networks": [
     *     {
     *          "network": {
     *              "id": 5,
     *              "key": "network key",
     *              "name": "name of network",
     *              "description": "short description of network"
     *          }
     *     }
     *     ],
     *     "lastLogin": "1970-01-01 03:00:00.0"
     *}
     *</code>
     *
     * If success, response with status 200, if user is not found 400
     * @param id user id
     * @return
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(HiveRoles.ADMIN)
    @JsonPolicyApply(JsonPolicyDef.Policy.USER_PUBLISHED)
    public Response getUser(@PathParam("id") long id) {
        User user = userDAO.findUserWithNetworks(id);

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok().entity(UserResponse.createFromUser(user)).build();
    }


    /**
     * One needs to provide user resource in request body (all parameters are mandatory):
     *
     * <code>
     * {
     *     "login":"login"
     *     "role":0
     *     "status":0
     *     "password":"qwerty"
     * }
     * </code>
     *
     * In case of success server will provide following response with code 201
     *
     * <code>
     *     {
     *         "id": 1,
     *         "lastLogin": null
     *     }
     * </code>
     *
     * @return Empty body, status 201 if success, 403 if forbidden, 400 otherwise
     */
    @POST
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.USERS_LISTED)
    public Response insertUser(UserRequest user) {
        //neither we want left some params omitted
        if (user.getLogin() == null || user.getPassword() == null || user.getRole() == null
                || user.getStatus() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        //nor we want these parameters to be null
        if (user.getLogin().getValue() == null || user.getPassword().getValue() == null
                || user.getRole().getValue() == null || user.getStatus().getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (userService.findByLogin(user.getLogin().getValue()) != null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        User created = userService.createUser(user.getLogin().getValue(), user.getRoleEnum(), user.getStatusEnum(), user.getPassword().getValue());

        return Response.status(Response.Status.CREATED).entity(created).build();
    }


    /**
     * Updates user. One should specify following json to update user (none of parameters are mandatory, bot neither of them can be null):
     *
     * <code>
     * {
     *   "login": "login",
     *   "role": 0,
     *   "status": 0,
     *   "password": "password"
     * }
     * </code>
     *
     * role:  Administrator - 0, Client - 1
     * status: ACTIVE - 0 (normal state, user can logon) , LOCKED_OUT - 1 (locked for multiple login failures), DISABLED - 2 , DELETED - 3;
     * @param userId - id of user beign edited
     * @return empty response, status 201 if succeeded, 403 if action is forbidden, 400 otherwise
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(UserRequest user, @PathParam("id") long userId) {

        if (user.getLogin() != null) {
            User u = userService.findByLogin(user.getLogin().getValue());

            if (u != null && u.getId() != userId) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        }

        if (user.getLogin() != null && user.getLogin().getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (user.getPassword() != null && user.getPassword().getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (user.getRole() != null && user.getRole().getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (user.getStatus() != null && user.getStatus().getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        String loginValue = user.getLogin() == null ? null : user.getLogin().getValue();
        String passwordValue = user.getPassword() == null ? null : user.getPassword().getValue();

        userService.updateUser(userId, loginValue, user.getRoleEnum(), user.getStatusEnum(), passwordValue);

        return Response.status(Response.Status.CREATED).build();
    }

    /**
     * Deletes user by id
     * @param userId id of user to delete
     * @return empty response. state 204 in case of success, 404 if not found
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response deleteUser(@PathParam("id") long userId) {
        if (!userService.deleteUser(userId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * Method returns following body in case of success (status 200):
     *<code>
     *     {
     *       "id": 5,
     *       "key": "network_key",
     *       "name": "network name",
     *       "description": "short description of net"
     *     }
     *</code>
     *in case, there is no such network, or user, or user doesn't have access
     *
     * @param id user id
     * @param networkId network id
     */
    @GET
    @Path("/{id}/network/{networkId}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.NETWORKS_LISTED)
    public Response getNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {

        User existingUser = userDAO.findUserWithNetworks(id);

        if (existingUser == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        for (Network network : existingUser.getNetworks()) {
            if (network.getId() == networkId) {
                return Response.ok().entity(network).build();
            }
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Request body must be empty. Returns Empty body.
     * @param id user id
     * @param networkId network id
     */
    @PUT
    @Path("/{id}/network/{networkId}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {
        try {
            userService.assignNetwork(id, networkId);
        } catch (Exception e) {
            throw new NotFoundException();
        }
        return Response.status(Response.Status.CREATED).build();
    }

    /**
     *   Removes user permissions on network
     * @param id user id
     * @param networkId network id
     * @return Empty body. Status 204 in case of success, 404 otherwise
     */
    @DELETE
    @Path("/{id}/network/{networkId}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unassignNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {
        try {
            userService.unassignNetwork(id, networkId);
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * Returns current user with networks:
     *<code>
     *{
     *     "id": 2,
     *     "login": "login",
     *     "status": 0,
     *     "networks": [
     *     {
     *          "network": {
     *              "id": 5,
     *              "key": "network key",
     *              "name": "network name",
     *              "description": "short description of network"
     *          }
     *     }
     *     ],
     *     "lastLogin": "1970-01-01 03:00:00.0"
     *}
     *</code>
     *
     * Or empty body and 403 status in case of user is not logged on
     *
     */
    @GET
    @Path("/current")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.USER_PUBLISHED)
    public Response getCurrent() {
        String login = requestContext.getSecurityContext().getUserPrincipal().getName();
        if (login == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(userService.findUserWithNetworksByLogin(login)).build();
    }

    /**
     * Updates user currently logged on.
     * One should specify following json to update user (none of parameters are mandatory, bot neither of them can be null):
     *
     * <code>
     * {
     *   "login": "login",
     *   "role": 0,
     *   "status": 0,
     *   "password": "password"
     * }
     * </code>
     *
     * role:  Administrator - 0, Client - 1
     * status: ACTIVE - 0 (normal state, user can logon) , LOCKED_OUT - 1 (locked for multiple login failures), DISABLED - 2 , DELETED - 3;
     * @return empty response, status 201 if succeeded, 403 if action is forbidden, 400 otherwise
     */
    @PUT
    @Path("/current")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.USERS_LISTED)
    public Response updateCurrent(UserRequest ui) {

        String password = ui.getPassword().getValue();

        if (password == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String login = requestContext.getSecurityContext().getUserPrincipal().getName();

        if (login == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        User u = userService.findUserWithNetworksByLogin(login);

        userService.updatePassword(u.getId(), password);
        return Response.status(Response.Status.CREATED).build();
    }


}

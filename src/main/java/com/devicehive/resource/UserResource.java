package com.devicehive.resource;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.updates.UserUpdate;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.devicehive.configuration.Constants.*;

@Path("/user")
public interface UserResource {

    /**
     * This method will generate following output <p/> <code> [ { "id": 2, "login": "login", "role": 0, "status": 0,
     * "lastLogin": "1970-01-01 03:00:00.0" }, { "id": 3, "login": "login1", "role": 1, "status": 2, "lastLogin":
     * "1970-01-01 03:00:00.0" } ] </code>
     *
     * @param login        user login ignored, when loginPattern is specified
     * @param loginPattern login pattern (LIKE %VALUE%) user login will be ignored, if not null
     * @param role         User's role ADMIN - 0, CLIENT - 1
     * @param status       ACTIVE - 0 (normal state, user can logon) , LOCKED_OUT - 1 (locked for multiple login
     *                     failures), DISABLED - 2 , DELETED - 3;
     * @param sortField    either of "login", "loginAttempts", "role", "status", "lastLogin"
     * @param sortOrderSt  either ASC or DESC
     * @param take         like SQL LIMIT
     * @param skip         like SQL OFFSET
     * @return List of User
     */
    @GET
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_USER')")
    Response getUsersList(
            @QueryParam(LOGIN) String login,
            @QueryParam(LOGIN_PATTERN) String loginPattern,
            @QueryParam(ROLE) Integer role,
            @QueryParam(STATUS) Integer status,
            @QueryParam(SORT_FIELD) String sortField,
            @QueryParam(SORT_ORDER) String sortOrderSt,
            @QueryParam(TAKE) Integer take,
            @QueryParam(SKIP) Integer skip
    );

    /**
     * Method will generate following output: <p/> <code> { "id": 2, "login": "login", "status": 0, "networks": [ {
     * "network": { "id": 5, "key": "network key", "name": "name of network", "description": "short description of
     * network" } } ], "lastLogin": "1970-01-01 03:00:00.0" } </code> <p/> If success, response with status 200, if user
     * is not found 400
     *
     * @param userId user id
     */
    @GET
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'GET_CURRENT_USER')")
    Response getUser(@PathParam(ID) Long userId);

    @GET
    @Path("/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY', 'CLIENT') and hasPermission(null, 'GET_CURRENT_USER')")
    Response getCurrent();

    /**
     * One needs to provide user resource in request body (all parameters are mandatory): <p/> <code> { "login":"login"
     * "role":0 "status":0 "password":"qwerty" } </code> <p/> In case of success server will provide following response
     * with code 201 <p/> <code> { "id": 1, "lastLogin": null } </code>
     *
     * @return Empty body, status 201 if success, 403 if forbidden, 400 otherwise
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_USER')")
    @JsonPolicyDef(JsonPolicyDef.Policy.USERS_LISTED)
    Response insertUser(UserUpdate userToCreate);

    /**
     * Updates user. One should specify following json to update user (none of parameters are mandatory, bot neither of
     * them can be null): <p/> <code> { "login": "login", "role": 0, "status": 0, "password": "password" } </code> <p/>
     * role:  Administrator - 0, Client - 1 status: ACTIVE - 0 (normal state, user can logon) , LOCKED_OUT - 1 (locked
     * for multiple login failures), DISABLED - 2 , DELETED - 3;
     *
     * @param userId - id of user being edited
     * @return empty response, status 201 if succeeded, 403 if action is forbidden, 400 otherwise
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_USER')")
    Response updateUser(UserUpdate user, @PathParam("id") Long userId);

    @PUT
    @Path("/current")
    @Consumes(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY', 'CLIENT') and hasPermission(null, 'UPDATE_CURRENT_USER')")
    Response updateCurrentUser(UserUpdate user);

    /**
     * Deletes user by id
     *
     * @param userId id of user to delete
     * @return empty response. state 204 in case of success, 404 if not found
     */
    @DELETE
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_USER')")
    Response deleteUser(@PathParam("id") long userId);

    /**
     * Method returns following body in case of success (status 200): <code> { "id": 5, "key": "network_key", "name":
     * "network name", "description": "short description of net" } </code> in case, there is no such network, or user,
     * or user doesn't have access
     *
     * @param id        user id
     * @param networkId network id
     */
    @GET
    @Path("/{id}/network/{networkId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'GET_NETWORK')")
    Response getNetwork(@PathParam(ID) long id, @PathParam(NETWORK_ID) long networkId);

    /**
     * Request body must be empty. Returns Empty body.
     *
     * @param id        user id
     * @param networkId network id
     */
    @PUT
    @Path("/{id}/network/{networkId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_NETWORK')")
    Response assignNetwork(@PathParam(ID) long id, @PathParam(NETWORK_ID) long networkId);

    /**
     * Removes user permissions on network
     *
     * @param id        user id
     * @param networkId network id
     * @return Empty body. Status 204 in case of success, 404 otherwise
     */
    @DELETE
    @Path("/{id}/network/{networkId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_NETWORK')")
    Response unassignNetwork(@PathParam(ID) long id, @PathParam(NETWORK_ID) long networkId);
}

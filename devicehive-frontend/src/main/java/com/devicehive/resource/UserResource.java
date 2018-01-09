package com.devicehive.resource;

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

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.response.UserDeviceTypeResponse;
import com.devicehive.model.response.EntityCountResponse;
import com.devicehive.model.response.UserNetworkResponse;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.vo.UserVO;
import com.devicehive.vo.UserWithNetworkVO;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/user")
@Api(tags = {"User"}, description = "Represents a user to this API.", consumes="application/json")
@Produces({"application/json"})
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
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_USER')")
    @ApiOperation(value = "List users", notes = "Gets list of users.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns array of User resources in the response body.", response = UserVO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "If request parameters invalid"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    void list(
            @ApiParam(name = "login", value = "Filter by user login.")
            @QueryParam("login")
            String login,
            @ApiParam(name = "loginPattern", value = "Filter by user login pattern.")
            @QueryParam("loginPattern")
            String loginPattern,
            @ApiParam(name = "role", value = "Filter by user role. 0 is Administrator, 1 is Client.")
            @QueryParam("role")
            Integer role,
            @ApiParam(name = "status", value = "Filter by user status. 0 is Active, 1 is Locked Out, 2 is Disabled.")
            @QueryParam("status")
            Integer status,
            @ApiParam(name = "sortField", value = "Result list sort field.", allowableValues = "ID,Login")
            @QueryParam("sortField")
            String sortField,
            @ApiParam(name = "sortOrder", value = "Result list sort order. The sortField should be specified.", allowableValues = "ASC,DESC")
            @QueryParam("sortOrder")
            String sortOrderSt,
            @ApiParam(name = "take", value = "Number of records to take from the result list.", defaultValue = "20")
            @QueryParam("take")
            Integer take,
            @ApiParam(name = "skip", value = "Number of records to skip from the result list.", defaultValue = "0")
            @QueryParam("skip")
            Integer skip,
            @Suspended final AsyncResponse asyncResponse);

    /**
     * @param login        user login ignored, when loginPattern is specified
     * @param loginPattern login pattern (LIKE %VALUE%) user login will be ignored, if not null
     * @param role         User's role ADMIN - 0, CLIENT - 1
     * @param status       ACTIVE - 0 (normal state, user can logon) , LOCKED_OUT - 1 (locked for multiple login
     *                     failures), DISABLED - 2 , DELETED - 3;
     * @return Count of Users
     */
    @GET
    @Path("/count")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_USER')")
    @ApiOperation(value = "Count users", notes = "Gets count of users.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns the count of users, matching the filters.", response = EntityCountResponse.class, responseContainer = "Count"),
            @ApiResponse(code = 400, message = "If request parameters invalid"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    void count(
            @ApiParam(name = "login", value = "Filter by user login.")
            @QueryParam("login")
                    String login,
            @ApiParam(name = "loginPattern", value = "Filter by user login pattern.")
            @QueryParam("loginPattern")
                    String loginPattern,
            @ApiParam(name = "role", value = "Filter by user role. 0 is Administrator, 1 is Client.")
            @QueryParam("role")
                    Integer role,
            @ApiParam(name = "status", value = "Filter by user status. 0 is Active, 1 is Locked Out, 2 is Disabled.")
            @QueryParam("status")
                    Integer status,
            @ApiParam(name = "sortField", value = "Result list sort field.", allowableValues = "ID,Login")
            @Suspended final AsyncResponse asyncResponse);

    /**
     * Method will generate following output: <p/> <code> { "id": 2, "login": "login", "status": 0, "networks": [ {
     * "network": { "id": 5, "name": "name of network", "description": "short description of
     * network" } } ], "lastLogin": "1970-01-01 03:00:00.0" } </code> <p/> If success, response with status 200, if user
     * is not found 400
     *
     * @param userId user id
     */
    @GET
    @Path("/{id}")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_USER')")
    @ApiOperation(value = "Get user", notes = "Gets information about user and its assigned networks.\n" +
            "Only administrators are allowed to get information about any user. User-level accounts can only retrieve information about themselves.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns a User resource in the response body.", response = UserWithNetworkVO.class),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If user is not found")
    })
    Response getUser(
            @ApiParam(name = "id", value = "User identifier.", required = true)
            @PathParam("id")
            Long userId);

    @GET
    @Path("/current")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_CURRENT_USER')")
    @ApiOperation(value = "Get current user", notes = "Get information about the current user.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns a User resource in the response body.", response = UserWithNetworkVO.class),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 409, message = "If user is not signed in")
    })
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
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_USER')")
    @JsonPolicyDef(JsonPolicyDef.Policy.USERS_LISTED)
    @ApiOperation(value = "Create user", notes = "Creates new user.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 201, message = "If successful, this method returns a User resource in the response body.", response = UserVO.class),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    Response insertUser(
            @ApiParam(value = "User body", defaultValue = "{}", required = true)
            UserUpdate userToCreate);

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
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_USER')")
    @ApiOperation(value = "Update current user", notes = "Updates an existing user. \n" +
            "Only administrators are allowed to update any property of any user.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 404, message = "If user not found")
    })
    Response updateUser(
            @ApiParam(value = "User body", defaultValue = "{}", required = true)
            UserUpdate user,
            @ApiParam(name = "id", value = "User identifier.", required = true)
            @PathParam("id")
            Long userId);

    @PUT
    @Path("/current")
    @Consumes(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'UPDATE_CURRENT_USER')")
    @ApiOperation(value = "Update current user", notes = "Updates current user. \n" +
            "Only administrators are allowed to update any property of any user.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    Response updateCurrentUser(
            @ApiParam(value = "User body", defaultValue = "{}", required = true)
            UserUpdate user);

    /**
     * Deletes user by id
     *
     * @param userId id of user to delete
     * @return empty response. state 204 in case of success, 404 if not found
     */
    @DELETE
    @Path("/{id}")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_USER')")
    @ApiOperation(value = "Delete user", notes = "Deletes an existing user.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
    })
    Response deleteUser(
            @ApiParam(name = "id", value = "User identifier.", required = true)
            @PathParam("id")
            long userId);

    /**
     * Method returns following body in case of success (status 200): <code> { "id": 5, "name":
     * "network name", "description": "short description of net" } </code> in case, there is no such network, or user,
     * or user doesn't have access
     *
     * @param id        user id
     * @param networkId network id
     */
    @GET
    @Path("/{id}/network/{networkId}")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_NETWORK')")
    @ApiOperation(value = "Get user's network", notes = "Gets information about user/network association.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns a Network resource in the response body.", response = UserNetworkResponse.class),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If user or network not found")
    })
    Response getNetwork(
            @ApiParam(name = "id", value = "User identifier.", required = true)
            @PathParam("id")
            long id,
            @ApiParam(name = "networkId", value = "Network identifier.", required = true)
            @PathParam("networkId")
            long networkId);

    /**
     * Request body must be empty. Returns Empty body.
     *
     * @param id        user id
     * @param networkId network id
     */
    @PUT
    @Path("/{id}/network/{networkId}")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_NETWORK')")
    @ApiOperation(value = "Assign network", notes = "Associates network with the user.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If user or network not found")
    })
    Response assignNetwork(
            @ApiParam(name = "id", value = "User identifier.", required = true)
            @PathParam("id")
            long id,
            @ApiParam(name = "networkId", value = "Network identifier.", required = true)
            @PathParam("networkId")
            long networkId);

    /**
     * Removes user permissions on network
     *
     * @param id        user id
     * @param networkId network id
     * @return Empty body. Status 204 in case of success, 404 otherwise
     */
    @DELETE
    @Path("/{id}/network/{networkId}")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_NETWORK')")
    @ApiOperation(value = "Unassign network", notes = "Removes association between network and user.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If user or network not found.")
    })
    Response unassignNetwork(
            @ApiParam(name = "id", value = "User identifier.", required = true)
            @PathParam("id")
            long id,
            @ApiParam(name = "networkId", value = "Network identifier.", required = true)
            @PathParam("networkId")
            long networkId);

    /**
     * Method returns following body in case of success (status 200): <code> { "id": 5, "name":
     * "device type name", "description": "short description of device type" } </code> in case, there is no such
     * device type, or user, or user doesn't have access
     *
     * @param id            user id
     * @param deviceTypeId  device type id
     */
    @GET
    @Path("/{id}/devicetype/{deviceTypeId}")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_TYPE')")
    @ApiOperation(value = "Get user's device type", notes = "Gets information about user/devicetype association.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns a DeviceType resource in the response body.", response = UserDeviceTypeResponse.class),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If user or device type not found")
    })
    Response getDeviceType(
            @ApiParam(name = "id", value = "User identifier.", required = true)
            @PathParam("id")
                    long id,
            @ApiParam(name = "deviceTypeId", value = "Device type identifier.", required = true)
            @PathParam("deviceTypeId")
                    long deviceTypeId);

    /**
     * Method returns the collection of available Device Types in case of success (status 200): <code> [{ "id": 5, "name":
     * "device type name", "description": "short description of device type" }] </code>  and empty list in case, there is no available
     * device type, or user, or user doesn't have access
     *
     * @param id            user id
     */
    @GET
    @Path("/{id}/devicetype")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_DEVICE_TYPE')")
    @ApiOperation(value = "Get user's device types", notes = "Gets information about user's devicetypes association.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns a list of DeviceTypes resource in the response body.", response = UserDeviceTypeResponse.class),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If user not found")
    })
    void getDeviceTypes(
            @ApiParam(name = "id", value = "User identifier.", required = true)
            @PathParam("id")
                    long id,
            @Suspended final AsyncResponse asyncResponse);

    /**
     * Adds user permission on device type.
     * Request body must be empty. Returns Empty body.
     *
     * @param id            user id
     * @param deviceTypeId  device type id
     */
    @PUT
    @Path("/{id}/devicetype/{deviceTypeId}")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_DEVICE_TYPE')")
    @ApiOperation(value = "Assign device type", notes = "Associates device type with the user.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If user or device type not found")
    })
    Response assignDeviceType(
            @ApiParam(name = "id", value = "User identifier.", required = true)
            @PathParam("id")
                    long id,
            @ApiParam(name = "deviceTypeId", value = "Device type identifier.", required = true)
            @PathParam("deviceTypeId")
                    long deviceTypeId);

    /**
     * Removes user permissions on device type.
     *
     * @param id            user id
     * @param deviceTypeId  device type id
     * @return Empty body. Status 204 in case of success, 404 otherwise
     */
    @DELETE
    @Path("/{id}/devicetype/{deviceTypeId}")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_DEVICE_TYPE')")
    @ApiOperation(value = "Unassign device type", notes = "Removes association between device type and user.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If user or device type not found.")
    })
    Response unassignDeviceType(
            @ApiParam(name = "id", value = "User identifier.", required = true)
            @PathParam("id")
                    long id,
            @ApiParam(name = "deviceTypeId", value = "Device type identifier.", required = true)
            @PathParam("deviceTypeId")
                    long deviceTypeId);

    /**
     * Adds user permission for all device types.
     * Request body must be empty. Returns Empty body.
     *
     * @param id            user id
     */
    @PUT
    @Path("/{id}/devicetype/all")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_DEVICE_TYPE')")
    @ApiOperation(value = "Assign all device types", notes = "Gives user permission to access all device types")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If user or device type not found")
    })
    Response allowAllDeviceTypes(
            @ApiParam(name = "id", value = "User identifier.", required = true)
            @PathParam("id")
                    long id);

    /**
     * Removes user permissions to access all device types.
     *
     * @param id            user id
     * @return Empty body. Status 204 in case of success, 404 otherwise
     */
    @DELETE
    @Path("/{id}/devicetype/all")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_DEVICE_TYPE')")
    @ApiOperation(value = "Unassign all device types", notes = "Removes user permission to access all device types.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If user or device type not found.")
    })
    Response disallowAllDeviceTypes(
            @ApiParam(name = "id", value = "User identifier.", required = true)
            @PathParam("id")
                    long id);
}

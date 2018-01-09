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

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.response.EntityCountResponse;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.vo.DeviceVO;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST controller for devices: <i>/device</i>. See <a href="http://www.devicehive.com/restful#Reference/Device">DeviceHive
 * RESTful API: Device</a> for details.
 */
@Path("/device")
@Api(tags = {"Device"}, description = "Represents a device, a unit that runs microcode and communicates to this API.", consumes = "application/json")
@Produces({"application/json"})
public interface DeviceResource {
    String DEVICE_ID_CONTAINS_INVALID_CHARACTERS = "Device Id can only contain letters, digits and dashes.";

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/list"> DeviceHive RESTful API:
     * Device: list</a>
     *
     * @param name               Device name.
     * @param namePattern        Device name pattern.
     * @param networkId          Associated network identifier
     * @param networkName        Associated network name
     * @param sortField          Result list sort field. Available values are Name, Status and Network.
     * @param sortOrderSt        Result list sort order. Available values are ASC and DESC.
     * @param take               Number of records to take from the result list.
     * @param skip               Number of records to skip from the result list.
     * @return list of <a href="http://www.devicehive.com/restful#Reference/Device">Devices</a>
     */
    @GET
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE')")
    @ApiOperation(value = "List devices", notes = "Gets list of devices.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns array of Device resources in the response body.",
                    response = DeviceVO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "If request parameters invalid"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    void list(
            @ApiParam(name = "name", value = "Filter by device name.")
            @QueryParam("name")
            String name,
            @ApiParam(name = "namePattern", value = "Filter by device name pattern. In pattern wildcards '%' and '_' can be used.")
            @QueryParam("namePattern")
            String namePattern,
            @ApiParam(name = "networkId", value = "Filter by associated network identifier.")
            @QueryParam("networkId")
            Long networkId,
            @ApiParam(name = "networkName", value = "Filter by associated network name.")
            @QueryParam("networkName")
            String networkName,
            @ApiParam(name = "sortField", value = "Result list sort field.", allowableValues = "Name,Network,Status,Devicetype")
            @QueryParam("sortField")
            String sortField,
            @ApiParam(name = "sortOrder", value = "Result list sort order. The sortField should be specified.", allowableValues = "ASC,DESC")
            @QueryParam("sortOrder")
            String sortOrderSt,
            @ApiParam(name = "take", value = "Number of records to take from the result list.", defaultValue = "20")
            @QueryParam("take")
            @Min(0) @Max(Integer.MAX_VALUE)
            Integer take,
            @ApiParam(name = "skip", value = "Number of records to skip from the result list.", defaultValue = "0")
            @QueryParam("skip")
            @Min(0) @Max(Integer.MAX_VALUE)
            Integer skip,
            @Suspended final AsyncResponse asyncResponse);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/count"> DeviceHive RESTful API:
     * Device: list</a>
     *
     * @param name               Device name.
     * @param namePattern        Device name pattern.
     * @param networkId          Associated network identifier
     * @param networkName        Associated network name
     * @return Count of Devices
     */
    @GET
    @Path("/count")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE')")
    @ApiOperation(value = "Count devices", notes = "Gets count of devices.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns the count of devices, matching the filters.",
                    response = EntityCountResponse.class, responseContainer = "Count"),
            @ApiResponse(code = 400, message = "If request parameters invalid"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    void count(
            @ApiParam(name = "name", value = "Filter by device name.")
            @QueryParam("name")
                    String name,
            @ApiParam(name = "namePattern", value = "Filter by device name pattern. In pattern wildcards '%' and '_' can be used.")
            @QueryParam("namePattern")
                    String namePattern,
            @ApiParam(name = "networkId", value = "Filter by associated network identifier.")
            @QueryParam("networkId")
                    Long networkId,
            @ApiParam(name = "networkName", value = "Filter by associated network name.")
            @QueryParam("networkName")
                    String networkName,
            @Suspended final AsyncResponse asyncResponse);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/register">DeviceHive RESTful API:
     * Device: register</a> Registers a device. If device with specified identifier has already been registered, it gets
     * updated in case when valid key is provided in the authorization header.
     *
     * @param deviceUpdate In the request body, supply a Device resource. See <a href="http://www.devicehive
     *                     .com/restful#Reference/Device/register">
     * @param deviceId   Device unique identifier.
     * @return response code 201, if successful
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'REGISTER_DEVICE')")
    @ApiOperation(value = "Register device", notes = "Registers or updates a device. For initial device registration, only 'name' property is required.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    Response register(
            @ApiParam(value = "Device body", required = true, defaultValue = "{}")
            @JsonPolicyApply(JsonPolicyDef.Policy.DEVICE_SUBMITTED)
            DeviceUpdate deviceUpdate,
            @ApiParam(name = "id", value = "Device unique identifier.", required = true)
            @PathParam("id")
            @Pattern(regexp = "[a-zA-Z0-9-]+", message = DEVICE_ID_CONTAINS_INVALID_CHARACTERS)
            String deviceId);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/get">DeviceHive RESTful API:
     * Device: get</a> Gets information about device.
     *
     * @param deviceId Device unique identifier
     * @return If successful, this method returns a <a href="http://www.devicehive.com/restful#Reference/Device">Device</a>
     * resource in the response body.
     */
    @GET
    @Path("/{id}")
    @PreAuthorize("isAuthenticated() and hasPermission(#deviceId, 'GET_DEVICE')")
    @ApiOperation(value = "Get device", notes = "Gets information about device.",
            response = DeviceVO.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns a Device resource in the response body."),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If device is not found")
    })
    Response get(
            @ApiParam(name = "id", value = "Device unique identifier.", required = true)
            @PathParam("id")
            String deviceId);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/delete">DeviceHive RESTful API:
     * Device: delete</a> Deletes an existing device.
     *
     * @param deviceId Device unique identifier
     * @return If successful, this method returns an empty response body.
     */
    @DELETE
    @Path("/{id}")
    @PreAuthorize("isAuthenticated() and hasPermission(#deviceId, 'REGISTER_DEVICE')")
    @ApiOperation(value = "Delete device", notes = "Deletes an existing device.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If device is not found")
    })
    Response delete(
            @ApiParam(name = "id", value = "Device unique identifier.", required = true)
            @PathParam("id")
            String deviceId);
}

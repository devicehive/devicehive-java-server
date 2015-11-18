package com.devicehive.resource;

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.model.updates.DeviceUpdate;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
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

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/list"> DeviceHive RESTful API:
     * Device: list</a>
     *
     * @param name               Device name.
     * @param namePattern        Device name pattern.
     * @param status             Device status
     * @param networkId          Associated network identifier
     * @param networkName        Associated network name
     * @param deviceClassId      Associated device class identifier
     * @param deviceClassName    Associated device class name
     * @param deviceClassVersion Associated device class version
     * @param sortField          Result list sort field. Available values are Name, Status, Network and DeviceClass.
     * @param sortOrderSt        Result list sort order. Available values are ASC and DESC.
     * @param take               Number of records to take from the result list.
     * @param skip               Number of records to skip from the result list.
     * @return list of <a href="http://www.devicehive.com/restful#Reference/Device">Devices</a>
     */
    @GET
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE')")
    @ApiOperation(value = "List devices", notes = "Gets list of devices.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns array of Device resources in the response body.", response = Device.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "If request parameters invalid"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    Response list(
            @ApiParam(name = "name", value = "Filter by device name.")
            @QueryParam("name")
            String name,
            @ApiParam(name = "namePattern", value = "Filter by device name pattern.")
            @QueryParam("namePattern")
            String namePattern,
            @ApiParam(name = "status", value = "Filter by device status.")
            @QueryParam("status")
            String status,
            @ApiParam(name = "networkId", value = "Filter by associated network identifier.")
            @QueryParam("networkId")
            Long networkId,
            @ApiParam(name = "networkName", value = "Filter by associated network name.")
            @QueryParam("networkName")
            String networkName,
            @ApiParam(name = "deviceClassId", value = "Filter by associated device class identifier.")
            @QueryParam("deviceClassId")
            Long deviceClassId,
            @ApiParam(name = "deviceClassName", value = "Filter by associated device class name.")
            @QueryParam("deviceClassName")
            String deviceClassName,
            @ApiParam(name = "deviceClassVersion", value = "Filter by associated device class version.")
            @QueryParam("deviceClassVersion")
            String deviceClassVersion,
            @ApiParam(name = "sortField", value = "Result list sort field.", allowableValues = "Name,Status,Network,DeviceClass")
            @QueryParam("sortField")
            String sortField,
            @ApiParam(name = "sortOrder", value = "Result list sort order.", allowableValues = "ASC,DESC")
            @QueryParam("sortOrder")
            String sortOrderSt,
            @ApiParam(name = "take", value = "Number of records to take from the result list.", defaultValue = "20")
            @QueryParam("take")
            @Min(0) @Max(Integer.MAX_VALUE)
            Integer take,
            @ApiParam(name = "skip", value = "Number of records to skip from the result list.", defaultValue = "0")
            @QueryParam("skip")
            @Min(0) @Max(Integer.MAX_VALUE)
            Integer skip);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/register">DeviceHive RESTful API:
     * Device: register</a> Registers a device. If device with specified identifier has already been registered, it gets
     * updated in case when valid key is provided in the authorization header.
     *
     * @param deviceUpdate In the request body, supply a Device resource. See <a href="http://www.devicehive
     *                     .com/restful#Reference/Device/register">
     * @param deviceGuid   Device unique identifier.
     * @return response code 201, if successful
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'REGISTER_DEVICE')")
    @ApiOperation(value = "Register device", notes = "Registers or updates a device. For initial device registration, only 'name' and 'deviceClass' properties are required.")
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
            String deviceGuid);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/get">DeviceHive RESTful API:
     * Device: get</a> Gets information about device.
     *
     * @param guid Device unique identifier
     * @return If successful, this method returns a <a href="http://www.devicehive.com/restful#Reference/Device">Device</a>
     * resource in the response body.
     */
    @GET
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE')")
    @ApiOperation(value = "Get device", notes = "Gets information about device.",
            response = Device.class)
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
            String guid);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/delete">DeviceHive RESTful API:
     * Device: delete</a> Deletes an existing device.
     *
     * @param guid Device unique identifier
     * @return If successful, this method returns an empty response body.
     */
    @DELETE
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'REGISTER_DEVICE')")
    @ApiOperation(value = "Delete device", notes = "Deletes an existing device.")
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
            String guid);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/equipment">DeviceHive RESTful API:
     * Device: equipment</a> Gets current state of device equipment. The equipment state is tracked by framework and it
     * could be updated by sending 'equipment' notification with the following parameters: equipment: equipment code
     * parameters: current equipment state
     *
     * @param guid Device unique identifier.
     * @return If successful, this method returns array of the following structures in the response body. <table> <tr>
     * <td>Property Name</td> <td>Type</td> <td>Description</td> </tr> <tr> <td>id</td> <td>string</td>
     * <td>Equipment code.</td> </tr> <tr> <td>timestamp</td> <td>datetime</td> <td>Equipment state
     * timestamp.</td> </tr> <tr> <td>parameters</td> <td>object</td> <td>Current equipment state.</td> </tr>
     * </table>
     */
    @GET
    @Path("/{id}/equipment")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_STATE')")
    @ApiOperation(value = "Get device's equipment", notes = "Gets current state of device equipment.\n" +
            "The equipment state is tracked by framework and it could be updated by sending 'equipment' notification with the following parameters:\n" +
            "equipment: equipment code\n" +
            "parameters: current equipment state")
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns an array of DeviceEquipment resources in the response body.",
                    response = DeviceEquipment.class,
                    responseContainer = "List"),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If device is not found")
    })
    Response equipment(
            @ApiParam(name = "id", value = "Device unique identifier.", required = true)
            @PathParam("id")
            String guid);

    /**
     * Gets current state of device equipment. The equipment state is tracked by framework and it could be updated by
     * sending 'equipment' notification with the following parameters: equipment: equipment code parameters: current
     * equipment state
     *
     * @param guid device guid
     * @param code equipment code
     * @return If successful return equipment associated with code and device with following guid
     */
    @GET
    @Path("/{id}/equipment/{code}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_STATE')")
    @ApiOperation(value = "Get current state of equipment", notes = "Gets current state of device equipment by code.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns a DeviceEquipment resource in the response body.",
                    response = DeviceEquipment.class),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If device or equipment is not found.")
    })
    Response equipmentByCode(
            @ApiParam(name = "id", value = "Device unique identifier.", required = true)
            @PathParam("id")
            String guid,
            @ApiParam(name = "code", value = "Equipment code.", required = true)
            @PathParam("code")
            String code);
}

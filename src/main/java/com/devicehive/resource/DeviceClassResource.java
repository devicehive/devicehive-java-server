package com.devicehive.resource;

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.wordnik.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICECLASS_PUBLISHED;

/**
 * REST controller for device classes: <i>/DeviceClass</i>. See <a href="http://www.devicehive.com/restful#Reference/DeviceClass">DeviceHive
 * RESTful API: DeviceClass</a> for details.
 */
@Path("/device/class")
@Api(tags = {"device-class"})
public interface DeviceClassResource {

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/list"> DeviceHive RESTful API:
     * DeviceClass: list</a> Gets list of device classes.
     *
     * @param name        Device class name.
     * @param namePattern Device class name pattern.
     * @param version     Device class version.
     * @param sortField   Result list sort field. Available values are ID and Name.
     * @param sortOrderSt Result list sort order. Available values are ASC and DESC.
     * @param take        Number of records to take from the result list.
     * @param skip        Number of records to skip from the result list.
     * @return If successful, this method returns array of <a href="http://www.devicehive
     *         .com/restful#Reference/DeviceClass"> DeviceClass </a> resources in the response body.
     */
    @GET
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_DEVICE_CLASS')")
    @ApiOperation(value = "List device classes", notes = "Returns the list of device classes based on provided parameters")
    @ApiResponses({
            @ApiResponse(code = 400, message = "If request is malformed")
    })
    Response getDeviceClassList(
            @ApiParam(name = "name", value = "Device class name")
            @QueryParam("name")
            String name,
            @ApiParam(name = "namePattern", value = "Name pattern (e.g. %value%)")
            @QueryParam("namePattern")
            String namePattern,
            @ApiParam(name = "version", value = "Device class version")
            @QueryParam("version")
            String version,
            @ApiParam(name = "sortField", value = "Device class field to sort by")
            @QueryParam("sortField")
            String sortField,
            @ApiParam(name = "sortOrder", value = "Sort order", allowableValues = "ASC,DESC")
            @QueryParam("sortOrder")
            String sortOrderSt,

            @ApiParam(name = "take", value = "Limit param", defaultValue = "20")
            @QueryParam("take")
            @Min(0) @Max(Integer.MAX_VALUE)
            Integer take,

            @ApiParam(name = "skip", value = "Skip param", defaultValue = "0")
            @QueryParam("skip")
            Integer skip);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/get"> DeviceHive RESTful API:
     * DeviceClass: get</a> Gets information about device class and its equipment.
     *
     * @param id Device class identifier.
     * @return If successful, this method returns a <a href="http://www.devicehive .com/restful#Reference/DeviceClass">DeviceClass</a>
     *         resource in the response body.
     */
    @GET
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY', 'CLIENT') and hasPermission(null, 'MANAGE_DEVICE_CLASS')")
    @ApiOperation(value = "Get device class", notes = "Returns device class by id")
    @ApiResponses({
            @ApiResponse(code = 404, message = "If device class not found")
    })
    Response getDeviceClass(
            @ApiParam(name = "id", value = "Device class id", required = true)
            @PathParam("id")
            long id);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/insert"> DeviceHive RESTful
     * API: DeviceClass: insert</a> Creates new device class.
     *
     * @param insert In the request body, supply a DeviceClass resource. { "name" : "Device class display name. String."
     *               "version" : "Device class version. String." "isPermanent" : "Indicates whether device class is
     *               permanent. Permanent device classes could not be modified by devices during registration. Boolean."
     *               "offlineTimeout" : "If set, specifies inactivity timeout in seconds before the framework changes
     *               device status to 'Offline'. Device considered inactive when it does not send any notifications.
     *               Integer." "data" : "Device class data, a JSON object with an arbitrary structure." } name, version
     *               and isPermanent are required fields
     * @return If successful, this method returns a DeviceClass resource in the response body.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_DEVICE_CLASS')")
    @ApiOperation(value = "Create device class", notes = "Creates new device class")
    @ApiResponses({
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 403, message = "If device class with such name and version exists or principal doesn't have permissions")
    })
    Response insertDeviceClass(
            @ApiParam(value = "Device class body", required = true, defaultValue = "{}")
            DeviceClass insert);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/update"> DeviceHive RESTful
     * API: DeviceClass: update</a> Updates an existing device class.
     *
     * @param id     Device class identifier.
     * @param insert In the request body, supply a <a href="http://www.devicehive .com/restful#Reference/DeviceClass">DeviceClass</a>
     *               resource.
     * @return If successful, this method returns an empty response body.
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_DEVICE_CLASS')")
    @ApiOperation(value = "Update device class", notes = "Updates existing device class")
    @ApiResponses({
            @ApiResponse(code = 404, message = "If device class doesn't exist"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    Response updateDeviceClass(
            @ApiParam(name = "id", value = "Device class id", required = true)
            @PathParam("id")
            long id,

            @ApiParam(value = "Device class body", required = true, defaultValue = "{}")
            @JsonPolicyApply(DEVICECLASS_PUBLISHED)
            DeviceClassUpdate insert);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/delete"> DeviceHive RESTful
     * API: DeviceClass: delete</a> Deletes an existing device class by id.
     *
     * @param id Device class identifier.
     * @return If successful, this method returns an empty response body with 204 status
     */
    @DELETE
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_DEVICE_CLASS')")
    @ApiOperation(value = "Update device class", notes = "Updates existing device class")
    @ApiResponses({
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    Response deleteDeviceClass(
            @ApiParam(name = "id", value = "Device class id", required = true)
            @PathParam("id") long id);
}

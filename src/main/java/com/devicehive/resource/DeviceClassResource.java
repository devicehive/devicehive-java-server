package com.devicehive.resource;

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.updates.DeviceClassUpdate;
import io.swagger.annotations.*;
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
@Api(tags = {"DeviceClass"}, value = "Represents a device class which holds meta-information about devices.", consumes = "application/json")
@Produces({"application/json"})
public interface DeviceClassResource {

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/list"> DeviceHive RESTful API:
     * DeviceClass: list</a> Gets list of device classes.
     *
     * @param name        Device class name.
     * @param namePattern Device class name pattern.
     * @param sortField   Result list sort field. Available values are ID and Name.
     * @param sortOrderSt Result list sort order. Available values are ASC and DESC.
     * @param take        Number of records to take from the result list.
     * @param skip        Number of records to skip from the result list.
     * @return If successful, this method returns array of <a href="http://www.devicehive
     * .com/restful#Reference/DeviceClass"> DeviceClass </a> resources in the response body.
     */
    @GET
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_DEVICE_CLASS')")
    @ApiOperation(value = "List device classes", notes = "Gets list of device classes.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "If successful, this method returns array of DeviceClass resources in the response body.",
                    response = DeviceClass.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "If request parameters invalid"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    Response getDeviceClassList(
            @ApiParam(name = "name", value = "Filter by device class name.")
            @QueryParam("name")
            String name,
            @ApiParam(name = "namePattern", value = "Filter by device class name pattern.")
            @QueryParam("namePattern")
            String namePattern,
            @ApiParam(name = "sortField", value = "Result list sort field.", allowableValues = "ID,Name")
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
            Integer skip);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/get"> DeviceHive RESTful API:
     * DeviceClass: get</a> Gets information about device class and its equipment.
     *
     * @param id Device class identifier.
     * @return If successful, this method returns a <a href="http://www.devicehive .com/restful#Reference/DeviceClass">DeviceClass</a>
     * resource in the response body.
     */
    @GET
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY', 'CLIENT') and hasPermission(null, 'MANAGE_DEVICE_CLASS')")
    @ApiOperation(value = "Get device class", notes = "Gets information about device class and its equipment.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "If successful, this method returns a DeviceClass resource in the response body.",
                    response = DeviceClass.class),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If device class not found")
    })
    Response getDeviceClass(
            @ApiParam(name = "id", value = "Device class identifier.", required = true)
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
    @ApiOperation(value = "Create device class", notes = "Creates new device class.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "If successful, this method returns a DeviceClass resource in the response body.",
                    response = DeviceClass.class),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions or device class with same name exists.")
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
    @ApiOperation(value = "Update device class", notes = "Updates an existing device class.")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If device class is not found")
    })
    Response updateDeviceClass(
            @ApiParam(name = "id", value = "Device class identifier.", required = true)
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
    @ApiOperation(value = "Update device class", notes = "Deletes an existing device class.")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If access key is not found")
    })
    Response deleteDeviceClass(
            @ApiParam(name = "id", value = "Device class identifier.", required = true)
            @PathParam("id") long id);
}

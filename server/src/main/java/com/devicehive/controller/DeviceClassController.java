package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.service.DeviceClassService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.controller.converters.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.*;

/**
 * REST controller for device classes: <i>/DeviceClass</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceClass">DeviceHive RESTful API: DeviceClass</a> for details.
 */
@Path("/device/class")
@LogExecutionTime
public class DeviceClassController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceClassController.class);
    private DeviceClassService deviceClassService;

    @EJB
    public void setDeviceClassService(DeviceClassService deviceClassService) {
        this.deviceClassService = deviceClassService;
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/list"> DeviceHive RESTful API:
     * DeviceClass: list</a>
     * Gets list of device classes.
     *
     * @param name        Device class name.
     * @param namePattern Device class name pattern.
     * @param version     Device class version.
     * @param sortField   Result list sort field. Available values are ID and Name.
     * @param sortOrder   Result list sort order. Available values are ASC and DESC.
     * @param take        Number of records to take from the result list.
     * @param skip        Number of records to skip from the result list.
     * @return If successful, this method returns array of <a href="http://www.devicehive
     *         .com/restful#Reference/DeviceClass"> DeviceClass </a> resources in the response body.
     */
    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    public Response getDeviceClassList(
            @QueryParam("name") String name,
            @QueryParam("namePattern") String namePattern,
            @QueryParam("version") String version,
            @QueryParam("sortField") String sortField,
            @QueryParam("sortOrder") @SortOrder Boolean sortOrder,
            @QueryParam("take") @Min(0) @Max(Integer.MAX_VALUE) Integer take,
            @QueryParam("skip") Integer skip) {

        logger.debug("DeviceClass list requested");
        if (sortOrder == null) {
            sortOrder = true;
        }
        if (!"ID".equals(sortField) && !"Name".equals(sortField) && sortField != null) {
            logger.debug("DeviceClass list request failed. Bad request for sortField");
            return ResponseFactory
                    .response(Response.Status.BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        } else if (sortField != null){
            sortField = sortField.toLowerCase();
        }

        List<DeviceClass> result = deviceClassService.getDeviceClassList(name, namePattern, version, sortField,
                sortOrder, take, skip);
        logger.debug("DeviceClass list proceed result. Result list contains {} elements", result.size());

        return ResponseFactory.response(OK, result, DEVICECLASS_LISTED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/get"> DeviceHive RESTful API:
     * DeviceClass: get</a>
     * Gets information about device class and its equipment.
     *
     * @param id Device class identifier.
     * @return If successful, this method returns a <a href="http://www.devicehive
     *         .com/restful#Reference/DeviceClass">DeviceClass</a> resource in the response body.
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT})
    public Response getDeviceClass(@PathParam("id") long id) {

        logger.debug("Get device class by id requested");

        DeviceClass result = deviceClassService.getWithEquipment(id);

        if (result == null) {
            logger.info("No device class with id = {} found", id);
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), "DeviceClass with id = " + id + " not found."));
        }

        logger.debug("Requested device class found");

        return ResponseFactory.response(OK, result, DEVICECLASS_PUBLISHED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/insert"> DeviceHive RESTful
     * API: DeviceClass: insert</a>
     * Creates new device class.
     *
     * @param insert In the request body, supply a DeviceClass resource.
     *               {
     *               "name" : "Device class display name. String."
     *               "version" : "Device class version. String."
     *               "isPermanent" : "Indicates whether device class is permanent. Permanent device classes could
     *               not be modified by devices during registration. Boolean."
     *               "offlineTimeout" : "If set, specifies inactivity timeout in seconds before the framework
     *               changes device status to 'Offline'. Device considered inactive when it does not send any
     *               notifications. Integer."
     *               "data" : "Device class data, a JSON object with an arbitrary structure."
     *               }
     *               name, version and isPermanent are required fields
     * @return If successful, this method returns a DeviceClass resource in the response body.
     */
    @POST
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertDeviceClass(DeviceClass insert) {
        logger.debug("Insert device class requested");
        DeviceClass result = deviceClassService.addDeviceClass(insert);

        logger.debug("Device class inserted");
        return ResponseFactory.response(CREATED, result, DEVICECLASS_SUBMITTED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/update"> DeviceHive RESTful
     * API: DeviceClass: update</a>
     * Updates an existing device class.
     *
     * @param id     Device class identifier.
     * @param insert In the request body, supply a <a href="http://www.devicehive
     *               .com/restful#Reference/DeviceClass">DeviceClass</a> resource.
     * @return If successful, this method returns an empty response body.
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateDeviceClass(
            @PathParam("id") long id,
            @JsonPolicyApply(DEVICECLASS_PUBLISHED) DeviceClassUpdate insert) {
        logger.debug("Device class update requested for id {}", id);
        deviceClassService.update(id, insert);
        logger.debug("Device class updated. Id {}", id);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/delete"> DeviceHive RESTful
     * API: DeviceClass: delete</a>
     * Deletes an existing device class by id.
     *
     * @param id Device class identifier.
     * @return If successful, this method returns an empty response body with 204 status
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response deleteDeviceClass(@PathParam("id") long id) {
        logger.debug("Device class delete requested");
        deviceClassService.delete(id);
        logger.debug("Device class deleted");
        return ResponseFactory.response(NO_CONTENT);
    }

}
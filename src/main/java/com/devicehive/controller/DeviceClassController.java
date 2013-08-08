package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.dao.DuplicateEntryException;
import com.devicehive.exceptions.dao.HivePersistingException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.EquipmentUpdate;
import com.devicehive.service.DeviceClassService;
import com.devicehive.service.EquipmentService;
import com.devicehive.utils.RestParametersConverter;
import com.devicehive.utils.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * REST controller for device classes: <i>/DeviceClass</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceClass">DeviceHive RESTful API: DeviceClass</a> for details.
 */
@Path("/device")
public class DeviceClassController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceClassController.class);

    @Inject
    private DeviceClassService deviceClassService;


    @Inject
    private EquipmentService equipmentService;

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
    @Path("/class")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response getDeviceClassList(
            @QueryParam("name") String name,
            @QueryParam("namePattern") String namePattern,
            @QueryParam("version") String version,
            @QueryParam("sortField") String sortField,
            @QueryParam("sortOrder") String sortOrder,
            @QueryParam("take") Integer take,
            @QueryParam("skip") Integer skip) {
        Timer t = Timer.newInstance();

        logger.debug("DeviceClass list requested");

        Boolean sortOrderAsc = RestParametersConverter.isSortAsc(sortOrder);

        if (sortOrderAsc == null) {
            logger.debug("DeviceClass list request failed. Bad request for sortOrder");
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse(ErrorResponse.WRONG_SORT_ORDER_PARAM_MESSAGE));
        }

        if (!"ID".equals(sortField) && !"Name".equals(sortField) && sortField != null) {
            logger.debug("DeviceClass list request failed. Bad request for sortField");
            return ResponseFactory
                    .response(Response.Status.BAD_REQUEST, new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        }

        List<DeviceClass> result = deviceClassService.getDeviceClassList(name, namePattern, version, sortField,
                sortOrderAsc, take, skip);
        logger.debug("DeviceClass list proceed result. Result list contains " + result.size() + " elems");
        t.logMethodExecuted("DeviceClassController.getDeviceClassList");
        return ResponseFactory.response(Response.Status.OK, result, JsonPolicyDef.Policy.DEVICECLASS_LISTED);
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
    @Path("/class/{id}")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT})
    public Response getDeviceClass(@PathParam("id") long id) {
        Timer t = Timer.newInstance();
        logger.debug("Get device class by id requested");

        DeviceClass result = deviceClassService.getWithEquipment(id);

        if (result == null) {
            logger.info("No device class with id = " + id + "found");
            return ResponseFactory.response(Response.Status.NOT_FOUND,
                    new ErrorResponse("DeviceClass with id = " + id + " not found."));
        }

        logger.debug("Requested device class found");
        t.logMethodExecuted("DeviceClassController.getDeviceClass");
        return ResponseFactory.response(Response.Status.OK, result, JsonPolicyDef.Policy.DEVICECLASS_PUBLISHED);
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
    @Path("/class")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertDeviceClass(DeviceClass insert) {
        Timer t = Timer.newInstance();
        logger.debug("Insert device class requested");
        DeviceClass result;

        try {
            result = deviceClassService.addDeviceClass(insert);
        } catch (DuplicateEntryException ex) {
            logger.debug("Unable to insert device class. This device class is already exists");
            return ResponseFactory
                    .response(Response.Status.FORBIDDEN, new ErrorResponse("DeviceClass couldn't be created"));
        } catch (HivePersistingException ex) {
            logger.debug("Unable to insert device class. This device class is already exists");
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse(ex.getMessage()));
        }
        logger.debug("Device class inserted");
        t.logMethodExecuted("DeviceClassController.insertDeviceClass");
        return ResponseFactory.response(Response.Status.CREATED, result, JsonPolicyDef.Policy.DEVICECLASS_SUBMITTED);
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
    @Path("/class/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateDeviceClass(
            @PathParam("id") long id,
            @JsonPolicyApply(JsonPolicyDef.Policy.DEVICECLASS_PUBLISHED) DeviceClassUpdate insert) {
        Timer t = Timer.newInstance();
        logger.debug("Device class update requested");
        try {
            deviceClassService.update(id, insert);
        } catch (HiveException e) {
            logger.debug("Unable to update device class");
            return ResponseFactory.response(
                    Response.Status.NOT_FOUND,
                    new ErrorResponse("DeviceClass with id = " + id + " not found."));
        }
        logger.debug("Device class updated");
        t.logMethodExecuted("DeviceClassController.updateDeviceClass");
        return ResponseFactory.response(Response.Status.NO_CONTENT);
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
    @Path("/class/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response deleteDeviceClass(@PathParam("id") long id) {
        Timer t = Timer.newInstance();
        logger.debug("Device class delete requested");
        deviceClassService.delete(id);
        logger.debug("Device class deleted");
        t.logMethodExecuted("DeviceClassController.deleteDeviceClass");
        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }

    /**
     * Gets current state of device equipment.
     * <code>
     * [
     * {
     * "id":1,
     * "timestamp": "1970-01-01 00:00:00.0",
     * "parameters":{/ *custom json object* /}
     * },
     * {
     * "id":2,
     * "timestamp": "1970-01-01 00:00:00.0",
     * "parameters":{/ *custom json object* /}
     * }
     * ]
     * <p/>
     * </code>
     *
     * @param classId device class id
     * @param eqId    equipment id
     * @return
     */
    @GET
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response getEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId) {
        Timer t = Timer.newInstance();
        logger.debug("Device class's equipment get requested");
        Equipment result = equipmentService.getByDeviceClass(classId, eqId);

        if (result == null) {
            logger.debug("No equipment with id = " + eqId + " for device class with id = " + classId + " found");
            return ResponseFactory.response(
                    Response.Status.NOT_FOUND,
                    new ErrorResponse("Equipment with id = " + eqId + " not found"));
        }
        logger.debug("Device class's equipment get proceed successfully");
        t.logMethodExecuted("DeviceClassController.getEquipment");
        return ResponseFactory.response(Response.Status.OK, result, JsonPolicyDef.Policy.EQUIPMENTCLASS_PUBLISHED);
    }

    /**
     * Adds new equipment type to device class
     *
     * @param classId device class id
     * @return
     */
    @POST
    @Path("/class/{deviceClassId}/equipment")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertEquipment(@PathParam("deviceClassId") long classId, Equipment equipmentq) {
        Timer t = Timer.newInstance();
        logger.debug("Insert device class's equipment requested");
        Equipment result = deviceClassService.createEquipment(classId, equipmentq);
        logger.debug("New device class's equipment created");
        t.logMethodExecuted("DeviceClassController.insertEquipment");
        return ResponseFactory.response(Response.Status.CREATED, result, JsonPolicyDef.Policy.EQUIPMENTCLASS_SUBMITTED);
    }

    /**
     * Updates device class' equipment. None of following parameters are mandatory.
     * Parameters, if left unspecified, remains unchanged, instead setting parameter to
     * null will null corresponding value.
     * In following JSON
     * <p/>
     * name 	Equipment display name.
     * code 	Equipment code. It's used to reference particular equipment and it should be unique within a device class.
     * type 	Equipment type. An arbitrary string representing equipment capabilities.
     * data 	Equipment data, a JSON object with an arbitrary structure.
     * <p/>
     * <code>
     * {
     * "name": "equipment name",
     * "code": "equipment_code",
     * "type": "equipment_type",
     * "data": {/ * json object* /}
     * }
     * </code>
     *
     * @param classId         id of class
     * @param eqId            equipment id
     * @param equipmentUpdate Json  object
     * @return empty response with status 201 in case of success, empty response with status 404, if there's no such record
     */
    @PUT
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateEquipment(
            @PathParam("deviceClassId") long classId,
            @PathParam("id") long eqId,
            @JsonPolicyApply(JsonPolicyDef.Policy.EQUIPMENTCLASS_PUBLISHED) EquipmentUpdate equipmentUpdate) {
        Timer t = Timer.newInstance();
        logger.debug("Update device class's equipment requested");

        if (!equipmentService.update(equipmentUpdate, eqId, classId)) {
            logger.debug("Unable to update equipment. Equipment with id = " + eqId + " for device class with id = " +
                    classId + " not found");
            return ResponseFactory.response(Response.Status.NOT_FOUND,
                    new ErrorResponse(
                            "Equipment with id = " + eqId + " or DeviceClass id = " + classId + " not found"));
        }

        logger.debug("Update device class's equipment finished successfully");
        t.logMethodExecuted("DeviceClassController.updateEquipment");
        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }

    /**
     * Will cascade deletes specified equipment and all
     * data for this equipment for all devise of this type.
     *
     * @param classId Device class id
     * @param eqId    Equipment id
     * @return empty body, 204 if success, 404 if no record found
     */
    @DELETE
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId) {
        Timer t = Timer.newInstance();
        logger.debug("Delete device class's equipment requested");
        equipmentService.delete(eqId, classId);
        logger.debug("Delete device class's equipment finished");
        t.logMethodExecuted("DeviceClassController.deleteEquipment");
        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }
}

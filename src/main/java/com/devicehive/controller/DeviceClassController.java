package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.dao.DeviceClassDAO;
import com.devicehive.dao.EquipmentDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.dao.DuplicateEntryException;
import com.devicehive.exceptions.dao.HivePersistingException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.service.DeviceClassService;
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
    private EquipmentDAO equipmentDAO;
    @Inject
    private DeviceClassDAO deviceClassDAO;

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
        logger.info("DeviceClass list requested");
        boolean sortOrderAsc = true;
        if (sortOrder != null && !sortOrder.equals("DESC") && !sortOrder.equals("ASC")) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters"));
        }
        if ("DESC".equals(sortOrder)) {
            sortOrderAsc = false;
        }
        if (!"ID".equals(sortField) && !"Name".equals(sortField) && sortField != null) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters"));
        }

        List<DeviceClass> result = deviceClassDAO.getDeviceClassList(name, namePattern, version, sortField,
                sortOrderAsc, take, skip);
        logger.info("DeviceClass list proceed result. Result list contains " + result.size() + " elems");
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
        logger.info("Get device class by id requested");
        DeviceClass result = deviceClassService.getWithEquipment(id);

        if (result == null) {
            logger.info("No device class with id = " + id + "found");
            return ResponseFactory.response(
                    Response.Status.NOT_FOUND,
                    new ErrorResponse("DeviceClass with id = " + id + " not found."));
        }
        logger.info("Requested device class found");
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
        logger.info("Insert device class requested");
        DeviceClass result;

        try {
            result = deviceClassService.addDeviceClass(insert);
        } catch (DuplicateEntryException ex) {
            logger.info("Unable to insert device class. This device class is already exists");
            return ResponseFactory.response(Response.Status.FORBIDDEN, new ErrorResponse("DeviceClass couldn't be created"));
        } catch (HivePersistingException ex) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse(ex.getMessage()));
        }
        logger.info("Device class inserted");
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

        logger.info("Device class update requested");
        try {
            deviceClassService.update(id, insert);
        } catch (HiveException e) {
            logger.info("Unable to update device class");
            return ResponseFactory.response(
                    Response.Status.NOT_FOUND,
                    new ErrorResponse("DeviceClass with id = " + id + " not found."));
        }
        logger.info("Device class updated");
        return ResponseFactory.response(Response.Status.CREATED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/delete"> DeviceHive RESTful
     * API: DeviceClass: delete</a>
     * Deletes an existing device class.
     *
     * @param id Device class identifier.
     * @return If successful, this method returns an empty response body.
     */
    @DELETE
    @Path("/class/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response deleteDeviceClass(@PathParam("id") long id) {
        logger.info("Device class delete requested");
        deviceClassService.delete(id);
        logger.info("Device class deleted");
        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }

    @GET
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response getEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId) {
        logger.info("Device class's equipment requested");
        Equipment result = equipmentDAO.getByDeviceClass(classId, eqId);

        if (result == null) {
            logger.info("No equipment with id = " + eqId + " for device class with id = " + classId + " found");
            return ResponseFactory.response(
                    Response.Status.NOT_FOUND,
                    new ErrorResponse("Equipment with id = " + eqId + " not found"));
        }
        logger.info("Device class's equipment found");
        return ResponseFactory.response(Response.Status.OK, result, JsonPolicyDef.Policy.EQUIPMENTCLASS_PUBLISHED);
    }

    @POST
    @Path("/class/{deviceClassId}/equipment")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertEquipment(@PathParam("deviceClassId") long classId, Equipment equipmentq) {
        logger.info("Insert device class's equipment requested");
        Equipment result = deviceClassService.createEquipment(classId, equipmentq);
        logger.info("New device class's equipment created");
        return ResponseFactory.response(Response.Status.CREATED, result, JsonPolicyDef.Policy.EQUIPMENTCLASS_SUBMITTED);
    }

    @PUT
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateEquipment(
            @PathParam("deviceClassId") long classId,
            @PathParam("id") long eqId,
            @JsonPolicyApply(JsonPolicyDef.Policy.EQUIPMENTCLASS_PUBLISHED) Equipment equipment) {
        logger.info("Update device class's equipment requested");
        if (!equipmentDAO.update(equipment, eqId, classId)) {
            return ResponseFactory.response(
                    Response.Status.NOT_FOUND,
                    new ErrorResponse("Equipment with id = " + eqId + " or DeviceClass id = " + classId + " not found"));
        }
        logger.info("Update device class's equipment finished");
        return ResponseFactory.response(Response.Status.CREATED);
    }

    @DELETE
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId) {
        logger.info("Delete device class's equipment requested");
        if (!equipmentDAO.delete(eqId, classId)) {
            return ResponseFactory.response(
                    Response.Status.NOT_FOUND,
                    new ErrorResponse("Equipment with id = " + eqId + " or DeviceClass id = " + classId + " not found"));
        }
        logger.info("Delete device class's equipment finished");
        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }
}

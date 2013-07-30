package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.dao.EquipmentDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.service.DeviceClassService;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * REST controller for device classes: <i>/DeviceClass</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceClass">DeviceHive RESTful API: DeviceClass</a> for details.
 */
@Path("/device")
public class DeviceClassController {

    @Inject
    private DeviceClassService deviceClassService;
    @Inject
    private EquipmentDAO equipmentDAO;

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
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.DEVICECLASS_LISTED)
    public List<DeviceClass> getDeviceClassList(@QueryParam("name") String name,
                                                @QueryParam("namePattern") String namePattern,
                                                @QueryParam("version") String version,
                                                @QueryParam("sortField") String sortField,
                                                @QueryParam("sortOrder") String sortOrder,
                                                @QueryParam("take") Integer take,
                                                @QueryParam("skip") Integer skip
    ) {
        return deviceClassService.getDeviceClassList(name, namePattern, version, sortField, sortOrder, take, skip);
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
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.DEVICECLASS_PUBLISHED)
    public DeviceClass getDeviceClass(@PathParam("id") long id) {
        return deviceClassService.getWithEquipment(id);
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertDeviceClass(DeviceClass insert) {
        Response.ResponseBuilder responseBuilder = Response.status(Response.Status.CREATED);
        Annotation[] annotations = {new JsonPolicyApply.JsonPolicyApplyLiteral(JsonPolicyDef.Policy
                .DEVICECLASS_SUBMITTED)};
        return responseBuilder.entity(deviceClassService.addDeviceClass(insert), annotations).build();

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
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDeviceClass(@PathParam("id") long id,@JsonPolicyApply(JsonPolicyDef.Policy.DEVICECLASS_PUBLISHED) DeviceClassUpdate insert) {
        try {
            deviceClassService.update(id, insert);
        } catch (HiveException e) {
            throw new NotFoundException(e.getMessage());
        }
        return Response.status(Response.Status.CREATED).build();
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
        if (!deviceClassService.delete(id)) {
            throw new NotFoundException("device with id = " + id + " does not exists");
        }
        return Response.status(Response.Status.NO_CONTENT).build();

    }

    @GET
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @JsonPolicyApply(JsonPolicyDef.Policy.EQUIPMENTCLASS_PUBLISHED)
    public Equipment getEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId) {
        return equipmentDAO.getByDeviceClass(classId, eqId);
    }

    @POST
    @Path("/class/{deviceClassId}/equipment")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertEquipment(@PathParam("deviceClassId") long classId, Equipment eq) {
        DeviceClass dc = new DeviceClass();
        dc.setId(classId);
        eq.setDeviceClass(dc);
        Equipment result = equipmentDAO.create(eq);
        Annotation[] annotations = {new JsonPolicyApply.JsonPolicyApplyLiteral(JsonPolicyDef.Policy.EQUIPMENTCLASS_SUBMITTED)};
        return Response.status(Response.Status.CREATED).entity(result, annotations).build();
    }

    @PUT
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId,
                                     @JsonPolicyApply(JsonPolicyDef.Policy.EQUIPMENTCLASS_PUBLISHED) Equipment
                                             equipment) {
        if (!equipmentDAO.update(equipment, eqId, classId)) {
            throw new NotFoundException("equipment with id = " + eqId + " does not exists");
        }
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId) {
        if (!equipmentDAO.delete(eqId, classId)) {
            throw new NotFoundException("No equipment found with id = " + eqId + " associated with the " + classId);
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}

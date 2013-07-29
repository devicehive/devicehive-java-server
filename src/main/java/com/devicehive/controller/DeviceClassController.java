package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.dao.NoSuchRecordException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.service.DeviceClassService;
import com.devicehive.service.EquipmentService;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * TODO JavaDoc
 */
@Path("/device")
public class DeviceClassController {

    @Inject
    private DeviceClassService deviceClassService;
    @Inject
    private EquipmentService equipmentService;

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

    @GET
    @Path("/class/{id}")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT})
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.DEVICECLASS_PUBLISHED)
    public DeviceClass getDeviceClass(@PathParam("id") long id) {
        return deviceClassService.getWithEquipment(id);
    }

    @POST
    @Path("/class")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.DEVICECLASS_SUBMITTED)
    public DeviceClass insertDeviceClass(DeviceClass insert) {
        return deviceClassService.addDeviceClass(insert);
    }

    @PUT
    @Path("/class/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.DEVICECLASS_SUBMITTED)
    public Response updateDeviceClass(@PathParam("id") long id, DeviceClassUpdate insert) {
        try {
            deviceClassService.update(id, insert);
        } catch (HiveException e) {
            throw new NotFoundException(e.getMessage());
        }
        return Response.status(201).build();
    }

    @DELETE
    @Path("/class/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response deleteDeviceClass(@PathParam("id") long id) {
        try {
            deviceClassService.delete(id);
            return Response.status(204).build();
        } catch (NoSuchRecordException e) {
            throw new NotFoundException(e);
        }
    }

    @GET
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @JsonPolicyApply(JsonPolicyDef.Policy.EQUIPMENTCLASS_PUBLISHED)
    public Equipment getEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId) {
        return equipmentService.getEquipment(classId, eqId);
    }

    @POST
    @Path("/class/{deviceClassId}/equipment")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.EQUIPMENTCLASS_SUBMITTED)
    public Equipment insertEquipment(@PathParam("deviceClassId") long classId, Equipment eq) {

        DeviceClass dc = new DeviceClass();
        dc.setId(classId);
        eq.setDeviceClass(dc);

        return equipmentService.insertEquipment(eq);
    }

    @PUT
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.EQUIPMENTCLASS_SUBMITTED)
    public Equipment updateEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId,
                                     Equipment eq) {
        Equipment e = equipmentService.get(eqId);

        if (e == null || e.getDeviceClass() == null || e.getDeviceClass().getId() != classId) {
            throw new NotFoundException("No such Equipment");
        }
        if (eq.getName() != null) {
            e.setName(eq.getName());
        }
        if (eq.getCode() != null) {
            e.setCode(eq.getCode());
        }
        if (eq.getType() != null) {
            e.setType(eq.getType());
        }
        if (eq.getData() != null) {
            e.setData(eq.getData());
        }

        equipmentService.updateEquipment(e);

        return e;
    }

    @DELETE
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId) {
        Equipment e = equipmentService.getEquipment(classId, eqId);
        if (e == null) {
            throw new NotFoundException("No such Equipment");
        }
        equipmentService.deleteEquipment(e);
        return Response.ok().build();
    }
}

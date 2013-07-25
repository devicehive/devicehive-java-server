package com.devicehive.controller;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.devicehive.exceptions.dao.NoSuchRecordException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.service.DeviceClassService;
import com.devicehive.service.EquipmentService;

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
    @RolesAllowed("ADMIN")
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
    @RolesAllowed({"ADMIN", "CLIENT"})
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.DEVICECLASS_PUBLISHED)
    public DeviceClass getDeviceClass(@PathParam("id") long id) {
        return deviceClassService.getWithEquipment(id);
    }

    @POST
    @Path("/class")
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.DEVICECLASS_SUBMITTED)
    public DeviceClass insertDeviceClass(DeviceClass insert) {
        return deviceClassService.addDeviceClass(insert);
    }

    @PUT
    @Path("/class/{id}")
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.DEVICECLASS_SUBMITTED)
    public Response updateDeviceClass(@PathParam("id") long id, DeviceClass insert) {
        insert.setId(id);
        deviceClassService.update(insert);
        return Response.ok().build();
    }

    @DELETE
    @Path("/class/{id}")
    @RolesAllowed("ADMIN")
    public Response deleteDeviceClass(@PathParam("id") long id) {
        try {
            deviceClassService.delete(id);
            return Response.ok().build();
        } catch (NoSuchRecordException e) {
            throw new NotFoundException(e);
        }
    }


    @GET
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed("ADMIN")
    @JsonPolicyApply(JsonPolicyDef.Policy.EQUIPMENTCLASS_PUBLISHED)
    public Equipment getEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId) {
        return equipmentService.getEquipment(classId, eqId);
    }


    @POST
    @Path("/class/{deviceClassId}/equipment")
    @RolesAllowed("ADMIN")
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
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.EQUIPMENTCLASS_SUBMITTED)
    public Equipment updateEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId, Equipment eq) {
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
    @RolesAllowed("ADMIN")
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

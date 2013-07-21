package com.devicehive.controller;

import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.request.EquipmentInsert;
import com.devicehive.model.response.SimpleEquipmentResponse;
import com.devicehive.service.EquipmentService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * TODO JavaDoc
 */
public class EquipmentController {

    @Inject
    private EquipmentService equipmentService;

    @GET
    @Path("/device/class/{deviceClassId}/equipment/{id}")
    public SimpleEquipmentResponse getEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId) {

        SimpleEquipmentResponse response = new SimpleEquipmentResponse();

        Equipment e = equipmentService.getEquipmentForDevice(classId, eqId);
        response.setId(e.getId());
        response.setName(e.getName());
        response.setCode(e.getCode());
        response.setData(e.getData());
        response.setType(e.getType());

        return response;
    }


    @POST
    @Path("/device/class/{deviceClassId}/equipment")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SimpleEquipmentResponse insertEquipment(@PathParam("deviceClassId") long classId, EquipmentInsert eq) {

        Equipment e = new Equipment();
        e.setType(eq.getType());
        e.setData(eq.getData());
        e.setCode(eq.getCode());
        e.setName(eq.getName());
        DeviceClass dc = new DeviceClass();
        dc.setId(classId);
        e.setDeviceClass(dc);
        e = equipmentService.insertEquipment(e);

        return SimpleEquipmentResponse.fromEquipment(e);
    }

    @PUT
    @Path("/device/class/{deviceClassId}/equipment/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SimpleEquipmentResponse updateEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId, EquipmentInsert eq) {
        Equipment e = equipmentService.get(eqId);

        if (e == null || e.getDeviceClass().getId() != classId) {
            throw new NotFoundException("No such Equipment");
        }

        e.setName(eq.getName());
        e.setCode(eq.getCode());
        e.setType(eq.getType());
        e.setData(eq.getData());
        equipmentService.updateEquipment(e);

        return SimpleEquipmentResponse.fromEquipment(e);
    }

    @DELETE
    @Path("/device/class/{deviceClassId}/equipment/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId) {
        Equipment e = equipmentService.getEquipmentForDevice(classId, eqId);
        if (e == null) {
            throw new NotFoundException("No such Equipment");
        }
        equipmentService.deleteEquipment(e);
        return Response.ok().build();
    }
}

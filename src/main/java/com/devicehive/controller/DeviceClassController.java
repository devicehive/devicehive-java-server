package com.devicehive.controller;

import com.devicehive.dao.DeviceClassDAO;
import com.devicehive.exceptions.dao.NoSuchRecordException;
import com.devicehive.model.DeviceClass;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO JavaDoc
 */
@Path("/device")
public class DeviceClassController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceClassController.class);

    @Inject
    private DeviceClassDAO deviceClassDAO;

    @Inject
    private DeviceClassService deviceClassService;

    @Inject
    private EquipmentService equipmentService;

    @GET
    @Path("/class")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeviceClass> getDeviceClassList() {
        return deviceClassDAO.getList();
    }

    @GET
    @Path("/class/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceClass getDeviceClass(@PathParam("id") long id) {
        return deviceClassDAO.getDeviceClass(id);
    }

    @POST
    @Path("/class")
    @Consumes(MediaType.APPLICATION_JSON)
    public long insertDeviceClass() {
        DeviceClass deviceClass = new DeviceClass();

        deviceClass.copyFieldsFrom(insert);

        return DeviceClassSimpleResponse.fromDeviceClass(deviceClassService.addDeviceClass(deviceClass));
    }

    @PUT
    @Path("/class/{id}")
    public Response updateDeviceClass(@PathParam("id") long id) {
        return Response.ok().build();
    }

    @DELETE
    @Path("/class/{id}")
    public Response deleteDeviceClass(@PathParam("id") long id) {
        //TODO: implement case with existing equipment
        try{
            deviceClassService.delete(id);
            return Response.ok().build();
        }catch(NoSuchRecordException e){
            throw new NotFoundException(e);
        }
    }


    @GET
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed("Administrator")
    public SimpleEquipmentResponse getEquipment(@PathParam("deviceClassId") long classId, @PathParam("id") long eqId) {

        SimpleEquipmentResponse response = new SimpleEquipmentResponse();

        Equipment e = equipmentService.getEquipment(classId, eqId);

        if(e == null) {
            throw new NotFoundException("No such equipment");
        }

        response.setId(e.getId());
        response.setName(e.getName());
        response.setCode(e.getCode());
        response.setData(e.getData());
        response.setType(e.getType());

        return response;
    }


    @POST
    @Path("/class/{deviceClassId}/equipment")
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
    @Path("/class/{deviceClassId}/equipment/{id}")
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
    @Path("/class/{deviceClassId}/equipment/{id}")
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

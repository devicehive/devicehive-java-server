package com.devicehive.controller;

import com.devicehive.dao.DeviceClassDAO;
import com.devicehive.exceptions.dao.NoSuchRecordException;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.request.DeviceClassInsert;
import com.devicehive.model.request.EquipmentInsert;
import com.devicehive.model.response.DetailedDeviceClassResponse;
import com.devicehive.model.response.DeviceClassSimpleResponse;
import com.devicehive.model.response.SimpleEquipmentResponse;
import com.devicehive.service.DeviceClassService;
import com.devicehive.service.EquipmentService;

import javax.annotation.security.RolesAllowed;
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

    @Inject
    private DeviceClassDAO deviceClassDAO;

    @Inject
    private DeviceClassService deviceClassService;

    @Inject
    private EquipmentService equipmentService;

    @GET
    @Path("/class")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeviceClassSimpleResponse> getDeviceClassList(@QueryParam("name") String name,
                                                @QueryParam("namePattern") String namePattern,
                                                @QueryParam("version") String version,
                                                @QueryParam("sortField") String sortField,
                                                @QueryParam("sortOrder") String sortOrder,
                                                @QueryParam("take") Integer take,
                                                @QueryParam("skip") Integer skip
                                                ) {
        List<DeviceClass> deviceClassList = deviceClassService.getDeviceClassList(name, namePattern, version, sortField, sortOrder, take, skip);

        List<DeviceClassSimpleResponse> result = new ArrayList<>();

        for(DeviceClass record:deviceClassList) {
            result.add(DeviceClassSimpleResponse.fromDeviceClass(record));
        }

        return result;
    }

    @GET
    @Path("/class/{id}")
    @RolesAllowed({"ADMIN","CLIENT"})
    @Produces(MediaType.APPLICATION_JSON)
    public DetailedDeviceClassResponse getDeviceClass(@PathParam("id") long id) {

        DeviceClass deviceClass = deviceClassService.getWithEquipment(id);

        DetailedDeviceClassResponse result = new DetailedDeviceClassResponse();
        result.setId(deviceClass.getId());
        result.setVersion(deviceClass.getVersion());
        result.setData(deviceClass.getData());
        result.setPermanent(deviceClass.getPermanent());
        result.setOfflineTimeout(deviceClass.getOfflineTimeout());
        result.setName(deviceClass.getName());
        Set<SimpleEquipmentResponse> equipmentResponseSet = new HashSet<>();

        for(Equipment e:deviceClass.getEquipment()){
            SimpleEquipmentResponse r = new SimpleEquipmentResponse();
            r.setId(e.getId());
            r.setName(e.getName());
            r.setCode(e.getCode());
            r.setType(e.getType());
            r.setData(e.getData());
            equipmentResponseSet.add(r);
        }

        result.setEquipment(equipmentResponseSet);

        return result;
    }

    @POST
    @Path("/class")
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceClassSimpleResponse insertDeviceClass(DeviceClassInsert insert) {

        DeviceClass deviceClass = new DeviceClass();

        deviceClass.copyFieldsFrom(insert);

        return DeviceClassSimpleResponse.fromDeviceClass(deviceClassService.addDeviceClass(deviceClass));
    }

    @PUT
    @Path("/class/{id}")
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceClassSimpleResponse updateDeviceClass(@PathParam("id") long id,DeviceClassInsert insert) {
        DeviceClass deviceClass = deviceClassService.getWithEquipment(id);
        deviceClass.copyFieldsFromOmmitingNulls(insert);
        deviceClassService.update(deviceClass);
        return DeviceClassSimpleResponse.fromDeviceClass(deviceClass);
    }

    @DELETE
    @Path("/class/{id}")
    @RolesAllowed("ADMIN")
    public Response deleteDeviceClass(@PathParam("id") long id) {
        try{
            deviceClassService.delete(id);
            return Response.ok().build();
        }catch(NoSuchRecordException e){
            throw new NotFoundException(e);
        }
    }


    @GET
    @Path("/class/{deviceClassId}/equipment/{id}")
    @RolesAllowed("ADMIN")
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
    @RolesAllowed("ADMIN")
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
    @RolesAllowed("ADMIN")
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

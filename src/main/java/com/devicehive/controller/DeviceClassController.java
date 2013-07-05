package com.devicehive.controller;

import com.devicehive.dao.DeviceClassDAO;
import com.devicehive.model.DeviceClass;

import javax.ejb.EJB;
import javax.ejb.Stateless;
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
    private DeviceClassDAO deviceClassDAO;

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
        deviceClass.setName("test");
        deviceClass.setPermanent(false);
        deviceClass.setVersion("1.0");
        return deviceClassDAO.addDeviceClass(deviceClass);
    }

    @PUT
    @Path("/class/{id}")
    public Response updateDeviceClass(@PathParam("id") long id) {
        return Response.ok().build();
    }

    @DELETE
    @Path("/class/{id}")
    public Response deleteDeviceClass(@PathParam("id") long id) {
        return Response.ok().build();
    }
}

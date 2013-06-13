package com.devicehive.controller;

import com.devicehive.dao.DeviceClassDAO;
import com.devicehive.model.DeviceClass;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * TODO JavaDoc
 */
@Path("/device")
public class DeviceClassController {
    @Inject
    public DeviceClassDAO deviceClassDAO;

    @GET
    @Path("/class")
    public Response getDeviceList() {
        deviceClassDAO.getList();
        return Response.ok().build();
    }

    public Response getDevice() {
        return Response.ok().build();
    }

    @POST
    @Path("/class")
    public Response insertDevice(DeviceClass deviceClass) {
        deviceClassDAO.addDeviceClass(deviceClass);
        return Response.ok().build();
    }

    public Response updateDevice() {
        return Response.ok().build();
    }

    public Response deleteDevice() {
        return Response.ok().build();
    }
}

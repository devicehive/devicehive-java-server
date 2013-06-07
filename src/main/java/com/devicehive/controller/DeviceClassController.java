package com.devicehive.controller;

import com.devicehive.dao.DeviceClassDAO;

import javax.inject.Inject;
import javax.ws.rs.GET;
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
    public Response getDeviceList() {
        deviceClassDAO.getList();
        return Response.ok().build();
    }
}

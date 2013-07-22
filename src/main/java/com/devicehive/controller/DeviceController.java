package com.devicehive.controller;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * TODO JavaDoc
 */
@Path("/")
public class DeviceController {

    @GET
    @Path("/device")
    public Response getDeviceList() {
        return Response.ok().build();
    }

    @GET
    @Path("/device/{id}")
    public Response getDevice() {
        return Response.ok().build();
    }

    @PUT
    @Path("/device/{id}")
    public Response registerDevice() {
        return Response.ok().build();
    }

    @DELETE
    @Path("/device/{id}")
    public Response deleteDevice() {
        return Response.ok().build();
    }

    @GET
    @Path("/device/{id}/equipment")
    public Response getDeviceEquipment() {
        return Response.ok().build();
    }
}

package com.devicehive.controller;

import com.devicehive.service.DeviceService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * TODO JavaDoc
 */
@Path("/device")
public class DeviceController {

    @Inject
    private DeviceService deviceService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@QueryParam("name") String name,
                         @QueryParam("namePattern") String namePattern,
                         @QueryParam("status") String status,
                         @QueryParam("networkId") Long networkId,
                         @QueryParam("networkName") String networkName,
                         @QueryParam("deviceClassId") Long deviceClassId,
                         @QueryParam("deviceClassName") String deviceClassName,
                         @QueryParam("deviceClassVersion") String deviceClassVersion,
                         @QueryParam("sortField") String sortField,
                         @QueryParam("sortOrder") String sortOrder,
                         @QueryParam("take") Integer take,
                         @QueryParam("skip") Integer skip) {

        return Response.ok().build();

    }

    public Response getDeviceList() {
        return Response.ok().build();
    }
}

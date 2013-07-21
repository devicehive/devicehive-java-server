package com.devicehive.controller;

import com.devicehive.dao.DeviceClassDAO;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * TODO JavaDoc
 */
@Path("/")
public class DeviceClassController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceClassController.class);

    @Inject
    private DeviceClassDAO deviceClassDAO;

    @GET
    @Path("/device/class")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public List<DeviceClass> getDeviceClassList() {
        logger.debug("DeviceClassList requested");
        return deviceClassDAO.getList();
    }

    @GET
    @Path("/device/class/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "CLIENT"})
    public DeviceClass getDeviceClass(@PathParam("id") long id) {
        return deviceClassDAO.getDeviceClass(id);
    }

    @POST
    @Path("/device/class")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public long insertDeviceClass() {
        DeviceClass deviceClass = new DeviceClass();
        deviceClass.setName("test");
        deviceClass.setPermanent(false);
        deviceClass.setVersion("1.0");
        return deviceClassDAO.addDeviceClass(deviceClass);
    }

    @PUT
    @Path("/device/class/{id}")
    @RolesAllowed("ADMIN")
    public Response updateDeviceClass(@PathParam("id") long id) {
        return Response.ok().build();
    }

    @DELETE
    @Path("/device/class/{id}")
    @RolesAllowed("ADMIN")
    public Response deleteDeviceClass(@PathParam("id") long id) {
        return Response.ok().build();
    }
}

package com.devicehive.controller;

import com.devicehive.dao.DeviceCommandDAO;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

/**
 * TODO JavaDoc
 */
//@Path("/device")
@Stateless
public class DeviceCommandController {

    @EJB
    private DeviceCommandDAO deviceCommandDAO;

    public Response getDeviceList() {
        return Response.ok().build();
    }
//
//    @GET
//    @Path("/{deviceGuid}/command")
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_JSON)
//    public List<DeviceCommand> queue(@PathParam("deviceGuid")UUID deviceGuid){           //UUID or String
//        //gson?
//        return deviceCommandDAO.queryDeviceCommand();
//    }


}

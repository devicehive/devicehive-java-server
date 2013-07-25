package com.devicehive.controller;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.model.DeviceCommand;

/**
 * TODO JavaDoc
 */
public class DeviceNotificationController {

    @GET
    @RolesAllowed({ "Client", "Device", "ADMIN" })
    @Path("/device/{deviceGuid}/notification/poll")
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(Policy.COMMAND_TO_DEVICE)
    public List<DeviceCommand> poll(
            @PathParam("deviceGuid") String deviceGuid,
            @QueryParam("timestamp") String timestampUTC,
            @QueryParam("waitTimeout") String waitTimeout) {

        return null;
    }

    @GET
    @RolesAllowed({ "Client", "Device", "ADMIN" })
    @Path("/device/notification/poll")
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(Policy.COMMAND_TO_DEVICE)
    public List<DeviceCommand> pollMany(
            @QueryParam("deviceGuids") String deviceGuids,
            @QueryParam("timestamp") String timestampUTC,
            @QueryParam("waitTimeout") String waitTimeout) {

        return null;
    }
}

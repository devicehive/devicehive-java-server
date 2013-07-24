package com.devicehive.controller;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.messages.bus.local.LocalMessageBus;
import com.devicehive.messages.bus.local.MessageBus;
import com.devicehive.messages.bus.local.PollResult;
import com.devicehive.messages.util.Params;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.MessageType;

/**
 * TODO JavaDoc
 */
//@Path("/device")
@Stateless
public class DeviceCommandController {

    @EJB
    private DeviceCommandDAO deviceCommandDAO;
    @EJB
    private DeviceDAO deviceDAO;
    @EJB
    private MessageBus messageBus;

    public Response getDeviceList() {
        return Response.ok().build();
    }

    @GET
    @Path("/{deviceGuid}/command/poll")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeviceCommand> poll(@PathParam("deviceGuid") String deviceGuid,
            @QueryParam("timestamp") String timestampUTC,
            @QueryParam("waitTimeout") String waitTimeout) {

        if (deviceGuid == null) {
            return Collections.emptyList();//TODO: error here
        }

        Device device = deviceDAO.findByUUID(UUID.fromString(deviceGuid));
        if (device == null) {
            return Collections.emptyList();//TODO: error here
        }

        Date timestamp = Params.parseUTCDate(timestampUTC);
        long timeout = Params.parseWaitTimeout(waitTimeout);

        PollResult result = messageBus.poll(MessageType.CLIENT_TO_DEVICE_COMMAND, timestamp, device.getId());
        List<DeviceCommand> response = LocalMessageBus.expandPollResult(result, timeout, DeviceCommand.class);

        return response;
    }
}

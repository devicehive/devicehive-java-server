package com.devicehive.controller;

import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.messages.bus.LocalMessageBus;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.messages.bus.PollResult;
import com.devicehive.messages.util.Params;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.MessageType;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * TODO JavaDoc
 */
@Path("/device")
public class DeviceCommandController {

    @Inject
    private DeviceCommandDAO commandDAO;
    @Inject
    private DeviceDAO deviceDAO;
    @Inject
    private MessageBus messageBus;

    @GET
    @Path("/{deviceGuid}/command")
    @RolesAllowed({"Client", "Administrator"})
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.COMMAND_TO_DEVICE)
    public List<DeviceCommand> query(@PathParam("deviceGuid") String guid,
                                     @QueryParam("start") String start,
                                     @QueryParam("end") String end,
                                     @QueryParam("command") String command,
                                     @QueryParam("status") String status,
                                     @QueryParam("sortField") String sortField,
                                     @QueryParam("sortOrder") String sortOrder,
                                     @QueryParam("take") Integer take,
                                     @QueryParam("skip") Integer skip) {
        if (sortOrder != null && (!sortOrder.equals("DESC") || !sortOrder.equals("ASC"))) {
            throw new BadRequestException("The sort order cannot be equal " + sortOrder);
        }
        boolean sortOrderAsc = true;
        if ("DESC".equals(sortOrder)) {
            sortOrderAsc = false;
        }
        if (!"Timestamp".equals(sortField) && !"Command".equals(sortField) && !"Status".equals(sortField) && sortField
                != null) {
            throw new BadRequestException("The sort field cannot be equal " + sortField);
        }
        if (sortField == null) {
            sortField = "timestamp";
        }
        sortField = sortField.toLowerCase();
        Timestamp startTimestamp = null, endTimestamp = null;
        try {
            if (start != null) {
                startTimestamp = Timestamp.valueOf(start);
            }
            if (end != null) {
                endTimestamp = Timestamp.valueOf(end);
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("start and end dat must be in format yyyy-[m]m-[d]d hh:mm:ss[.f...]");
        }
        Device device = getDevice(guid);
        return commandDAO.queryDeviceCommand(device, startTimestamp, endTimestamp, command, status, sortField,
                sortOrderAsc,
                take, skip);
    }



    @GET
    @PermitAll//TODO: What roles are allowed here? @RolesAllowed({"Client", "ADMIN"})
    @Path("/poll")
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(Policy.COMMAND_TO_DEVICE)
    public List<DeviceCommand> poll(
            @PathParam("deviceGuid") String deviceGuid,
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

    private Device getDevice(String uuid) {
        UUID deviceId;
        try {
            deviceId = UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("unparseable guid: " + uuid);
        }

        Device device = deviceDAO.findByUUID(deviceId);
        if (device == null) {
            throw new NotFoundException("device with guid " + uuid + " not found");
        }

        return device;
    }
}

package com.devicehive.controller;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.devicehive.auth.UserPrincipal;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.messages.MessageDetails;
import com.devicehive.messages.MessageType;
import com.devicehive.messages.bus.DeferredResponse;
import com.devicehive.messages.bus.LocalMessageBus;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.messages.util.Params;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.User;

/**
 * REST controller for device commands: <i>/device/{deviceGuid}/command</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceCommand">DeviceHive RESTful API: DeviceCommand</a> for details.
 */
@Path("/device/{deviceGuid}/command")
public class DeviceCommandController {

    @Inject
    private DeviceDAO deviceDAO;
    @Inject
    private MessageBus messageBus;
    @Context
    private SecurityContext securityContext;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand/poll">DeviceHive RESTful API: DeviceCommand: poll</a>
     * 
     * @param deviceGuid Device unique identifier.
     * @param timestampUTC Timestamp of the last received command (UTC). If not specified, the server's timestamp is taken instead.
     * @param waitTimeout Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable waiting.
     * @return Array of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand">DeviceCommand</a>
     */
    @GET
    @PermitAll
    //@RolesAllowed({ "Client", "Device", "ADMIN" })
    @Path("/poll")
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(Policy.COMMAND_TO_DEVICE)
    public List<DeviceCommand> poll(
            @PathParam("deviceGuid") String deviceGuid,
            @QueryParam("timestamp") String timestampUTC,
            @QueryParam("waitTimeout") String waitTimeout) {

        if (deviceGuid == null) {
            throw new NotFoundException();
        }

        Device device = deviceDAO.findByUUID(UUID.fromString(deviceGuid));
        if (device == null) {
            throw new NotFoundException();
        }

        Date timestamp = Params.parseUTCDate(timestampUTC);
        long timeout = Params.parseWaitTimeout(waitTimeout);

        User user = ((UserPrincipal) securityContext.getUserPrincipal()).getUser();
        DeferredResponse result = messageBus.subscribe(MessageType.CLIENT_TO_DEVICE_COMMAND,
                MessageDetails.create().ids(device.getId()).timestamp(timestamp).user(user));
        List<DeviceCommand> response = LocalMessageBus.expandDeferredResponse(result, timeout, DeviceCommand.class);

        return response;
    }
}

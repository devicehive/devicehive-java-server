package com.devicehive.controller;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
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
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.lang.annotation.Annotation;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for device commands: <i>/device/{deviceGuid}/command</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceCommand">DeviceHive RESTful API: DeviceCommand</a> for details.
 */
@Path("/device/{deviceGuid}/command")
public class DeviceCommandController {

    @Inject
    private DeviceCommandDAO commandDAO;
    @Inject
    private DeviceCommandService commandService;
    @Inject
    private DeviceService deviceService;
    @Inject
    private DeviceDAO deviceDAO;
    @Inject
    private MessageBus messageBus;
    @Inject
    private UserDAO userDAO;
    @Context
    private ContainerRequestContext requestContext;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand/poll">DeviceHive RESTful API: DeviceCommand: poll</a>
     *
     * @param deviceGuid   Device unique identifier.
     * @param timestampUTC Timestamp of the last received command (UTC). If not specified, the server's timestamp is taken instead.
     * @param waitTimeout  Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable waiting.
     * @return Array of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand">DeviceCommand</a>
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Path("/poll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response poll(
            @PathParam("deviceGuid") String deviceGuid,
            @QueryParam("timestamp") String timestampUTC,
            @QueryParam("waitTimeout") String waitTimeout,
            @Context SecurityContext securityContext) {

        if (deviceGuid == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Device device = deviceDAO.findByUUID(UUID.fromString(deviceGuid));
        if (device == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Date timestamp = Params.parseUTCDate(timestampUTC);
        long timeout = Params.parseWaitTimeout(waitTimeout);

        User user = ((HivePrincipal) securityContext.getUserPrincipal()).getUser();
        DeferredResponse result = messageBus.subscribe(MessageType.CLIENT_TO_DEVICE_COMMAND,
                MessageDetails.create().ids(device.getId()).timestamp(timestamp).user(user));
        List<DeviceCommand> response = LocalMessageBus.expandDeferredResponse(result, timeout, DeviceCommand.class);
        Annotation[] annotations = {new JsonPolicyApply.JsonPolicyApplyLiteral(Policy.COMMAND_TO_DEVICE)};
        return Response.ok().entity(response, annotations).build();
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand/wait">DeviceHive RESTful API: DeviceCommand: wait</a>
     *
     * @param waitTimeout Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable waiting.
     * @return One of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand">DeviceCommand</a>
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Path("/{commandId}/poll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response wait(
            @PathParam("deviceGuid") String deviceGuid,
            @PathParam("commandId") String commandId,
            @QueryParam("waitTimeout") String waitTimeout,
            @Context SecurityContext securityContext) {

        if (deviceGuid == null || commandId == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Device device = deviceDAO.findByUUID(UUID.fromString(deviceGuid));
        if (device == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        DeviceCommand command = commandDAO.findById(Long.valueOf(commandId));
        if (command == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!command.getDevice().getId().equals(device.getId())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        long timeout = Params.parseWaitTimeout(waitTimeout);

        User user = ((HivePrincipal) securityContext.getUserPrincipal()).getUser();
        DeferredResponse result = messageBus.subscribe(MessageType.DEVICE_TO_CLIENT_UPDATE_COMMAND,
                MessageDetails.create().ids(device.getId(), command.getId()).user(user));
        List<DeviceCommand> commandList = LocalMessageBus.expandDeferredResponse(result, timeout, DeviceCommand.class);
        Annotation[] annotations = {new JsonPolicyApply.JsonPolicyApplyLiteral(Policy.COMMAND_TO_DEVICE)};
        DeviceCommand response = commandList.isEmpty() ? null : commandList.get(0);
        return Response.ok().entity(response, annotations).build();
    }

    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(@PathParam("deviceGuid") String guid,
                          @QueryParam("start") String start,
                          @QueryParam("end") String end,
                          @QueryParam("command") String command,
                          @QueryParam("status") String status,
                          @QueryParam("sortField") String sortField,
                          @QueryParam("sortOrder") String sortOrder,
                          @QueryParam("take") Integer take,
                          @QueryParam("skip") Integer skip) {
        if (sortOrder != null && !sortOrder.equals("DESC") && !sortOrder.equals("ASC")) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        boolean sortOrderAsc = true;
        if ("DESC".equals(sortOrder)) {
            sortOrderAsc = false;
        }
        if (!"Timestamp".equals(sortField) && !"Command".equals(sortField) && !"Status".equals(sortField) && sortField
                != null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (sortField == null) {
            sortField = "timestamp";
        }
        sortField = sortField.toLowerCase();
        Date startTimestamp = null, endTimestamp = null;
        if (start != null) {
            startTimestamp = Params.parseUTCDate(start);
            if (startTimestamp == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }
        if (end != null) {
            endTimestamp = Params.parseUTCDate(end);
            if (endTimestamp == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }
        Device device = getDevice(guid);
        Annotation[] annotations = {new JsonPolicyApply.JsonPolicyApplyLiteral(Policy.COMMAND_TO_DEVICE)};
        List<DeviceCommand> commandList = commandDAO.queryDeviceCommand(device, startTimestamp, endTimestamp, command,
                status, sortField, sortOrderAsc, take, skip);
        return Response.ok().entity(commandList, annotations).build();
    }

    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response get(@PathParam("deviceGuid") String guid, @PathParam("id") Long id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(guid);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        DeviceCommand result = commandService.getByGuidAndId(uuid, id);
        if (result == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Annotation[] annotations = {new JsonPolicyApply.JsonPolicyApplyLiteral(Policy.COMMAND_TO_DEVICE)};
        return Response.ok().entity(result, annotations).build();
    }

    @POST
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insert(@PathParam("deviceGuid") String guid, DeviceCommand deviceCommand) {
        Device device = getDevice(guid);
        String login = requestContext.getSecurityContext().getUserPrincipal().getName();

        if (login == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        User u = userDAO.findUserWithNetworksByLogin(login);
        deviceService.submitDeviceCommand(deviceCommand, device, u, null);
        Annotation[] annotations =
                {new JsonPolicyApply.JsonPolicyApplyLiteral(JsonPolicyDef.Policy.POST_COMMAND_TO_DEVICE)};
        return Response.status(Response.Status.CREATED).entity(deviceCommand, annotations).build();

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

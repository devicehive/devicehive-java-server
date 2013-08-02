package com.devicehive.controller;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.messages.MessageDetails;
import com.devicehive.messages.MessageType;
import com.devicehive.messages.bus.DeferredResponse;
import com.devicehive.messages.bus.LocalMessageBus;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.messages.util.Params;
import com.devicehive.model.*;
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
import java.util.Date;
import java.util.List;
import java.util.Set;
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
    public Response poll(
            @PathParam("deviceGuid") String deviceGuid,
            @QueryParam("timestamp") String timestampUTC,
            @QueryParam("waitTimeout") String waitTimeout,
            @Context SecurityContext securityContext) {

        if (deviceGuid == null) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST);
        }

        Device device = deviceDAO.findByUUID(UUID.fromString(deviceGuid));
        if (device == null) {
            return ResponseFactory.response(Response.Status.NOT_FOUND);
        }

        Date timestamp = Params.parseUTCDate(timestampUTC);
        long timeout = Params.parseWaitTimeout(waitTimeout);

        User user = ((HivePrincipal) securityContext.getUserPrincipal()).getUser();
        DeferredResponse result = messageBus.subscribe(MessageType.CLIENT_TO_DEVICE_COMMAND,
                MessageDetails.create().ids(device.getId()).timestamp(timestamp).user(user));
        List<DeviceCommand> response = LocalMessageBus.expandDeferredResponse(result, timeout, DeviceCommand.class);
        return ResponseFactory.response(Response.Status.OK, response, Policy.COMMAND_TO_DEVICE);
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
    public Response wait(
            @PathParam("deviceGuid") String deviceGuid,
            @PathParam("commandId") String commandId,
            @QueryParam("waitTimeout") String waitTimeout,
            @Context SecurityContext securityContext) {

        if (deviceGuid == null || commandId == null) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST);
        }

        Device device = deviceDAO.findByUUID(UUID.fromString(deviceGuid));
        if (device == null) {
            return ResponseFactory.response(Response.Status.NOT_FOUND);
        }

        DeviceCommand command = commandDAO.findById(Long.valueOf(commandId));
        if (command == null) {
            return ResponseFactory.response(Response.Status.NOT_FOUND);
        }

        if (!command.getDevice().getId().equals(device.getId())) {
            ResponseFactory.response(Response.Status.BAD_REQUEST);
        }

        long timeout = Params.parseWaitTimeout(waitTimeout);

        User user = ((HivePrincipal) securityContext.getUserPrincipal()).getUser();
        DeferredResponse result = messageBus.subscribe(MessageType.DEVICE_TO_CLIENT_UPDATE_COMMAND,
                MessageDetails.create().ids(device.getId(), command.getId()).user(user));
        List<DeviceCommand> commandList = LocalMessageBus.expandDeferredResponse(result, timeout, DeviceCommand.class);
        DeviceCommand response = commandList.isEmpty() ? null : commandList.get(0);
        return ResponseFactory.response(Response.Status.OK, response, Policy.COMMAND_TO_DEVICE);
    }

    /**
     * Example response:
     *
     * <code>
     * [
     * {
     * "id": 1
     * "timestamp": 	"1970-01-01 00:00:00.0",
     * "userId": 	1,
     * "command": 	"command_name",
     * "parameters": 	{/ *command parameters* /},
     * "lifetime": 10,
     * "flags":0,
     * "status":"device_status",
     * "result":{/ * result, JSON object* /}
     * },
     * {
     * "id": 2
     * "timestamp": 	"1970-01-01 00:00:00.0",
     * "userId": 	1,
     * "command": 	"command_name",
     * "parameters": 	{/ * command parameters * /},
     * "lifetime": 10,
     * "flags":0,
     * "status":"device_status",
     * "result":{/ * result, JSON object* /}
     * }
     * ]
     * </code>
     *
     * @param guid      UUID, string like "550e8400-e29b-41d4-a716-446655440000"
     * @param start     start date in format "yyyy-MM-dd'T'HH:mm:ss.SSS"
     * @param end       end date in format "yyyy-MM-dd'T'HH:mm:ss.SSS"
     * @param command   filter by command
     * @param status    filter by status
     * @param sortField either "Timestamp", "Command" or "Status"
     * @param sortOrder ASC or DESC
     * @param take      like mysql LIMIT
     * @param skip      like mysql OFFSET
     * @return list of device command with status 200, otherwise empty response with status 400
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN})
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
            return ResponseFactory.response(Response.Status.BAD_REQUEST);
        }
        boolean sortOrderAsc = true;
        if ("DESC".equals(sortOrder)) {
            sortOrderAsc = false;
        }
        if (!"Timestamp".equals(sortField) && !"Command".equals(sortField) && !"Status".equals(sortField) && sortField
                != null) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST);
        }
        if (sortField == null) {
            sortField = "timestamp";
        }
        sortField = sortField.toLowerCase();
        Date startTimestamp = null, endTimestamp = null;
        if (start != null) {
            startTimestamp = Params.parseUTCDate(start);
            if (startTimestamp == null) {
                return ResponseFactory.response(Response.Status.BAD_REQUEST);
            }
        }
        if (end != null) {
            endTimestamp = Params.parseUTCDate(end);
            if (endTimestamp == null) {
                return ResponseFactory.response(Response.Status.BAD_REQUEST);
            }
        }
        Device device = getDevice(guid);
        List<DeviceCommand> commandList = commandDAO.queryDeviceCommand(device, startTimestamp, endTimestamp, command,
                status, sortField, sortOrderAsc, take, skip);
        return ResponseFactory.response(Response.Status.OK, commandList, Policy.COMMAND_TO_DEVICE);
    }

    /**
     * Response contains following output:
     * <p/>
     * <code>
     * {
     * "id": 	1
     * "timestamp": 	"1970-01-01 00:00:00.0"
     * "userId": 	1
     * "command": 	"command_name"
     * "parameters": 	{/ * JSON Object * /}
     * "lifetime": 	100
     * "flags": 	1
     * "status": 	"comand_status"
     * "result": 	{ / * JSON Object* /}
     * }
     * </code>
     *
     * @param guid String with Device UUID like "550e8400-e29b-41d4-a716-446655440000"
     * @param id   command id
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Path("/{id}")
    @JsonPolicyApply(Policy.COMMAND_TO_DEVICE)
    public Response get(@PathParam("deviceGuid") String guid, @PathParam("id") long id) {

        Device device = getDevice(guid);

        if (!checkPermissions(device)) {
            return ResponseFactory.response(Response.Status.FORBIDDEN);
        }

        DeviceCommand result = commandService.getByGuidAndId(device.getGuid(), id);

        if (result == null) {
            return ResponseFactory.response(Response.Status.NOT_FOUND);
        }

        return ResponseFactory.response(Response.Status.OK, result);
    }

    /**
     * <b>Creates new device command.</b>
     *
     * <i>Example request:</i>
     * <code>
     * {
     * "command": 	"command name",
     * "parameters": 	{/ * Custom Json Object * /},
     * "lifetime": 0,
     * "flags": 0
     * }
     * </code>
     * <p>
     * Where,
     * command 	is Command name, required
     * parameters 	Command parameters, a JSON object with an arbitrary structure. is not required
     * lifetime 	Command lifetime, a number of seconds until this command expires. is not required
     * flags 	Command flags, and optional value that could be supplied for device or related infrastructure. is not required\
     * </p>
     * <p>
     * <i>Example response:</i>
     * </p>
     * <code>
     * {
     * "id": 1,
     * "timestamp": "1970-01-01 00:00:00.0",
     * "userId": 	1
     * }
     * </code>
     *
     * @param guid
     * @param deviceCommand
     * @return
     */
    @POST
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insert(@PathParam("deviceGuid") String guid, DeviceCommand deviceCommand) {
        Device device = getDevice(guid);
        String login = requestContext.getSecurityContext().getUserPrincipal().getName();

        if (login == null) {
            return ResponseFactory.response(Response.Status.FORBIDDEN);
        }

        User u = userDAO.findUserWithNetworksByLogin(login);
        deviceService.submitDeviceCommand(deviceCommand, device, u, null);

        return ResponseFactory.response(Response.Status.CREATED, deviceCommand, Policy.POST_COMMAND_TO_DEVICE);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand/update">DeviceHive
     * RESTful API: DeviceCommand: update</a>
     * Updates an existing device command.
     *
     * @param guid      Device unique identifier.
     * @param commandId Device command identifier.
     * @param command   In the request body, supply a <a href="http://www.devicehive
     *                  .com/restful#Reference/DeviceCommand">DeviceCommand</a> resource.
     *                  All fields are not required:
     *                  flags - Command flags, and optional value that could be supplied for
     *                  device or related infrastructure.
     *                  status - Command status, as reported by device or related infrastructure.
     *                  result - Command execution result, an optional value that could be provided by device.
     * @return If successful, this method returns an empty response body.
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed({HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("deviceGuid") String guid, @PathParam("id") long commandId,
                           @JsonPolicyApply(Policy.REST_COMMAND_UPDATE_FROM_DEVICE) DeviceCommand command) {
        UUID deviceId;
        try {
            deviceId = UUID.fromString(guid);
        } catch (IllegalArgumentException e) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST, "unparseable guid: " + guid);
        }
        DeviceCommand commandUpdate = commandDAO.getByDeviceGuidAndId(deviceId, commandId);
        if (commandUpdate == null) {
            return ResponseFactory.response(Response.Status.FORBIDDEN, "no permissions for device with guid " + guid
                    + "to update command with id " + commandId);
        }
        commandUpdate.setFlags(command.getFlags());
        commandUpdate.setStatus(command.getStatus());
        commandUpdate.setResult(command.getResult());
        deviceService.submitDeviceCommandUpdate(commandUpdate, commandUpdate.getDevice());
        return ResponseFactory.response(Response.Status.CREATED);
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

    private boolean checkPermissions(Device device) {
        HivePrincipal principal = (HivePrincipal) requestContext.getSecurityContext().getUserPrincipal();
        if (principal.getDevice() != null) {

            if (!device.getGuid().equals(principal.getDevice().getGuid())) {
                return false;
            }

            if (device.getNetwork() == null) {
                return false;
            }

        } else {
            User user = principal.getUser();
            if (user.getRole().equals(UserRole.CLIENT)) {
                User userWithNetworks = userDAO.findUserWithNetworks(user.getId());
                Set<Network> networkSet = userWithNetworks.getNetworks();
                return networkSet.contains(device.getNetwork());
            }
        }
        return true;
    }
}

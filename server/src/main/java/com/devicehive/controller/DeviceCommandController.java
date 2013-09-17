package com.devicehive.controller;

import com.devicehive.auth.AllowedAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.messages.handler.RestHandlerCreator;
import com.devicehive.messages.subscriptions.*;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.service.UserService;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.utils.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.inject.Singleton;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.devicehive.auth.AllowedAction.Action.GET_DEVICE_COMMAND;

/**
 * REST controller for device commands: <i>/device/{deviceGuid}/command</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceCommand">DeviceHive RESTful API: DeviceCommand</a> for details.
 */
@Path("/device/{deviceGuid}/command")
@LogExecutionTime
@Singleton
public class DeviceCommandController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceCommandController.class);
    @EJB
    private DeviceCommandService commandService;
    @EJB
    private DeviceService deviceService;
    @EJB
    private UserService userService;
    @EJB
    private DeviceCommandDAO deviceCommandDAO;
    @EJB
    private DeviceCommandService deviceCommandService;
    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private TimestampService timestampService;
    private ExecutorService asyncPool;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand/poll">DeviceHive RESTful API: DeviceCommand: poll</a>
     *
     * @param deviceGuid Device unique identifier.
     * @param timestamp  Timestamp of the last received command (UTC). If not specified, the server's timestamp is taken instead.
     * @param timeout    Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable waiting.
     * @return Array of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand">DeviceCommand</a>
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedAction(action = {GET_DEVICE_COMMAND})
    @Path("/poll")
    public void poll(
            @PathParam("deviceGuid") final String deviceGuid,
            @QueryParam("timestamp") final Timestamp timestamp,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam("waitTimeout") final long timeout,
            @Context SecurityContext securityContext,
            @Suspended final AsyncResponse asyncResponse) {

        final HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                logger.debug("DeviceCommand poll proceed successfully. deviceid = {}", deviceGuid);
            }
        });
        asyncPool.submit(new Runnable() {
            @Override
            public void run() {
                pollAction(deviceGuid, timestamp, timeout, principal, asyncResponse);
            }
        });
    }

    private void pollAction(String deviceGuid, Timestamp timestamp, long timeout, HivePrincipal principal,
                            AsyncResponse asyncResponse) {
        logger.debug("DeviceCommand poll requested deviceId = {} timestamp = {} ", deviceGuid, timestamp);

        if (principal.getUser() != null) {
            logger.debug("DeviceCommand poll was requested by User = {}, deviceId = {}, timestamp = ",
                    principal.getUser().getLogin(), deviceGuid, timestamp);
        } else if (principal.getDevice() != null) {
            logger.debug("DeviceCommand poll was requested by Device = {}, deviceId = {}, timestamp = ",
                    principal.getDevice().getGuid(), deviceGuid, timestamp);
        } else if (principal.getKey() != null) {
            logger.debug("DeviceCommand poll was requested by Key = {}, deviceId = {}, timestamp = ",
                    principal.getKey().getId(), deviceGuid, timestamp);
        }

        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }
        List<DeviceCommand> list = getDeviceCommandsList(principal, deviceGuid, timestamp);

        if (list.isEmpty()) {
            logger.debug("Waiting for command for device = {}", deviceGuid);
            CommandSubscriptionStorage storage = subscriptionManager.getCommandSubscriptionStorage();
            String reqId = UUID.randomUUID().toString();
            RestHandlerCreator restHandlerCreator = new RestHandlerCreator();
            Device device = deviceService.getDevice(deviceGuid, principal.getUser(),
                    principal.getDevice());
            CommandSubscription commandSubscription =
                    new CommandSubscription(device.getId(), reqId, restHandlerCreator);

            if (SimpleWaiter
                    .subscribeAndWait(storage, commandSubscription, restHandlerCreator.getFutureTask(), timeout)) {
                list = getDeviceCommandsList(principal, deviceGuid, timestamp);
            }
        }
        Response response = ResponseFactory.response(Response.Status.OK, list, Policy.COMMAND_LISTED);
        asyncResponse.resume(response);
    }

    private List<DeviceCommand> getDeviceCommandsList(HivePrincipal principal, String guid, Timestamp timestamp) {

        User authUser = principal.getUser();
        if (authUser == null && principal.getKey() != null) {
            authUser = principal.getKey().getUser();
        }
        if (authUser != null && authUser.getRole().equals(UserRole.CLIENT)) {
            return deviceCommandDAO.getByUserAndDeviceNewerThan(guid, timestamp, authUser);
        }
        return deviceCommandDAO.getNewerThan(guid, timestamp);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand/wait">DeviceHive RESTful API: DeviceCommand: wait</a>
     *
     * @param timeout Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable waiting.
     * @return One of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand">DeviceCommand</a>
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedAction(action = {GET_DEVICE_COMMAND})
    @Path("/{commandId}/poll")
    public void wait(
            @PathParam("deviceGuid") final String deviceGuid,
            @PathParam("commandId") final Long commandId,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam("waitTimeout") final long timeout,
            @Context SecurityContext securityContext,
            @Suspended final AsyncResponse asyncResponse) {

        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        final User user = principal.getUser() != null ? principal.getUser() : principal.getKey().getUser();
        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                logger.debug("DeviceCommand poll proceed successfully. deviceid = {}. CommandId = {}", deviceGuid,
                        commandId);
            }
        });

        asyncPool.submit(new Runnable() {
            @Override
            public void run() {
                waitAction(deviceGuid, commandId, timeout, asyncResponse, user);
            }
        });
    }

    private void waitAction(String deviceGuid, Long commandId, long timeout, AsyncResponse asyncResponse, User user) {
        logger.debug("DeviceCommand wait requested, deviceId = {},  commandId = {}", deviceGuid, commandId);

        if (deviceGuid == null || commandId == null) {
            logger.debug("DeviceCommand wait request failed. Bad request for sortOrder.");
            Response response = ResponseFactory.response(Response.Status.BAD_REQUEST);
            asyncResponse.resume(response);
            return;
        }

        Device device = deviceService.findByUUID(deviceGuid, user);

        if (device == null) {
            logger.debug("DeviceCommand wait request failed. No device found with guid = {} ", deviceGuid);
            Response response = ResponseFactory.response(Response.Status.NOT_FOUND);
            asyncResponse.resume(response);
            return;
        }
        /*    No need to check user permissions on command.
         *    We'll fail request, if this command is not sent for device user has access to.
         */

        DeviceCommand command = commandService.findById(commandId);

        if (command == null) {
            logger.debug("DeviceCommand wait request failed. No command found with id = {} for deviceId = {} ",
                    commandId, deviceGuid);
            Response response = ResponseFactory.response(Response.Status.NOT_FOUND);
            asyncResponse.resume(response);
            return;
        }

        //command is not for requested device
        if (!command.getDevice().getId().equals(device.getId())) {
            logger.debug(
                    "DeviceCommand wait request failed. Command with id = {} was not sent for device with guid = {}",
                    commandId, deviceGuid);
            Response response = ResponseFactory.response(Response.Status.BAD_REQUEST);
            asyncResponse.resume(response);
            return;
        }

        if (command.getEntityVersion() == 0) {
            CommandUpdateSubscriptionStorage storage = subscriptionManager.getCommandUpdateSubscriptionStorage();
            String reqId = UUID.randomUUID().toString();
            RestHandlerCreator restHandlerCreator = new RestHandlerCreator();
            CommandUpdateSubscription commandSubscription =
                    new CommandUpdateSubscription(command.getId(), reqId, restHandlerCreator);


            if (SimpleWaiter
                    .subscribeAndWait(storage, commandSubscription, restHandlerCreator.getFutureTask(), timeout)) {
                command = commandService.findById(commandId);
            }
        }

        DeviceCommand response = command.getEntityVersion() > 0 ? command : null;
        Response result = ResponseFactory.response(Response.Status.OK, response, Policy.COMMAND_TO_DEVICE);
        asyncResponse.resume(result);
    }

    /**
     * Example response:
     * <p/>
     * <code>
     * [
     * {
     * "id": 1
     * "timestamp":     "1970-01-01 00:00:00.0",
     * "userId":    1,
     * "command":   "command_name",
     * "parameters":    {/ *command parameters* /},
     * "lifetime": 10,
     * "flags":0,
     * "status":"device_status",
     * "result":{/ * result, JSON object* /}
     * },
     * {
     * "id": 2
     * "timestamp":     "1970-01-01 00:00:00.0",
     * "userId":    1,
     * "command":   "command_name",
     * "parameters":    {/ * command parameters * /},
     * "lifetime": 10,
     * "flags":0,
     * "status":"device_status",
     * "result":{/ * result, JSON object* /}
     * }
     * ]
     * </code>
     *
     * @param guid      GUID, string like "550e8400-e29b-41d4-a716-446655440000"
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
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedAction(action = {GET_DEVICE_COMMAND})
    public Response query(@PathParam("deviceGuid") String guid,
                          @QueryParam("start") Timestamp start,
                          @QueryParam("end") Timestamp end,
                          @QueryParam("command") String command,
                          @QueryParam("status") String status,
                          @QueryParam("sortField") String sortField,
                          @QueryParam("sortOrder") @SortOrder Boolean sortOrder,
                          @QueryParam("take") Integer take,
                          @QueryParam("skip") Integer skip,
                          @Context SecurityContext securityContext) {

        logger.debug("Device command query requested");
        if (sortOrder == null) {
            sortOrder = true;
        }

        if (!"Timestamp".equals(sortField) && !"Command".equals(sortField) && !"Status".equals(sortField) &&
                sortField != null) {
            logger.debug("Device command query failed. Bad request for sortField.");
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        }

        if (sortField == null) {
            sortField = "timestamp";
        }

        sortField = sortField.toLowerCase();

        Device device;
        Set<Long> allowedNetworkIds = null;
        Set<String> allowedDeviceGuids = null;
        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        User user = principal.getUser();
        if (user == null && principal.getKey() != null) {
            user = principal.getKey().getUser();
            allowedNetworkIds = new HashSet<>();
            Set<AccessKeyPermission> accessKeyPermissions = principal.getKey().getPermissions();
            for (AccessKeyPermission currentPermission : accessKeyPermissions) {
                if (currentPermission.getNetworkIdsAsSet() == null) {
                    allowedNetworkIds.add(null);
                } else {
                    allowedNetworkIds.addAll(currentPermission.getNetworkIdsAsSet());
                }
            }
            allowedDeviceGuids = new HashSet<>();
            for (AccessKeyPermission currentPermission : accessKeyPermissions) {
                if (currentPermission.getDeviceGuidsAsSet() == null) {
                    allowedDeviceGuids.add(null);
                } else {
                    allowedDeviceGuids.addAll(currentPermission.getDeviceGuidsAsSet());
                }
            }
        }
        device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, user, principal.getDevice());
        if (allowedDeviceGuids!= null && !allowedDeviceGuids.contains(null) && !allowedDeviceGuids.contains(device.getGuid())) {
            logger.debug("Device command query failed. Device with guid {} not found for access key", guid);
            return ResponseFactory.response(Response.Status.NOT_FOUND,
                    new ErrorResponse("Device not found"));
        }

        if (allowedNetworkIds!= null && !allowedNetworkIds.contains(null) && !allowedNetworkIds.contains(device
                .getNetwork().getId())){
            logger.debug("Device command query failed. Device with guid {} not found for access key", guid);
            return ResponseFactory.response(Response.Status.NOT_FOUND,
                    new ErrorResponse("Device not found"));
        }

        List<DeviceCommand> commandList =
                commandService.queryDeviceCommand(device, start, end, command, status, sortField, sortOrder, take,
                        skip);

        logger.debug("Device command query request proceed successfully");
        return ResponseFactory.response(Response.Status.OK, commandList, Policy.COMMAND_LISTED);
    }

    /**
     * Response contains following output:
     * <p/>
     * <code>
     * {
     * "id":    1
     * "timestamp":     "1970-01-01 00:00:00.0"
     * "userId":    1
     * "command":   "command_name"
     * "parameters":    {/ * JSON Object * /}
     * "lifetime":  100
     * "flags":     1
     * "status":    "comand_status"
     * "result":    { / * JSON Object* /}
     * }
     * </code>
     *
     * @param guid String with Device GUID like "550e8400-e29b-41d4-a716-446655440000"
     * @param id   command id
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedAction(action = {GET_DEVICE_COMMAND})
    @Path("/{id}")
    public Response get(@PathParam("deviceGuid") String guid, @PathParam("id") long id,
                        @Context SecurityContext securityContext) {
        logger.debug("Device command get requested. deviceId = {}, commandId = {}", guid, id);

        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        User user = principal.getUser();
        if (user == null && principal.getKey() != null) {
            user = principal.getKey().getUser();
        }
        Device device = deviceService.getDevice(guid, user, principal.getDevice());

        DeviceCommand result = commandService.getByGuidAndId(device.getGuid(), id);

        if (result == null) {
            logger.debug("Device command get failed. No command with id = {} found for device with guid = {}", id,
                    guid);
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("Command Not Found"));
        }

        if (result.getUser() != null) {
            result.setUserId(result.getUser().getId());
        }

        logger.debug("Device command get proceed successfully deviceId = {} commandId = {}", guid, id);
        return ResponseFactory.response(Response.Status.OK, result, Policy.COMMAND_TO_DEVICE);
    }

    /**
     * <b>Creates new device command.</b>
     * <p/>
     * <i>Example request:</i>
     * <code>
     * {
     * "command":   "command name",
     * "parameters":    {/ * Custom Json Object * /},
     * "lifetime": 0,
     * "flags": 0
     * }
     * </code>
     * <p>
     * Where,
     * command  is Command name, required
     * parameters   Command parameters, a JSON object with an arbitrary structure. is not required
     * lifetime     Command lifetime, a number of seconds until this command expires. is not required
     * flags    Command flags, and optional value that could be supplied for device or related infrastructure. is not required\
     * </p>
     * <p>
     * <i>Example response:</i>
     * </p>
     * <code>
     * {
     * "id": 1,
     * "timestamp": "1970-01-01 00:00:00.0",
     * "userId":    1
     * }
     * </code>
     *
     * @param guid
     * @param deviceCommand
     */
    @POST
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedAction(action = {GET_DEVICE_COMMAND})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insert(@PathParam("deviceGuid") String guid,
                           @JsonPolicyApply(Policy.COMMAND_FROM_CLIENT) DeviceCommand deviceCommand,
                           @Context SecurityContext securityContext) {
        logger.debug("Device command insert requested. deviceId = {}, command = {}", guid, deviceCommand.getCommand());
        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        User user = principal.getUser() != null ? principal.getUser() : principal.getKey().getUser();
        String login = user.getLogin();

        if (login == null) {
            return ResponseFactory.response(Response.Status.FORBIDDEN);
        }

        User u = userService.findUserWithNetworksByLogin(login);

        Device device = deviceService.getDevice(guid, user, principal.getDevice());

        commandService.submitDeviceCommand(deviceCommand, device, u, null);
        deviceCommand.setUserId(u.getId());

        logger.debug("Device command insertAll proceed successfully. deviceId = {} commandId = {}", guid,
                deviceCommand.getId());
        return ResponseFactory.response(Response.Status.CREATED, deviceCommand, Policy.COMMAND_TO_CLIENT);
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
    @RolesAllowed({HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.KEY})
    @AllowedAction(action = {GET_DEVICE_COMMAND})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("deviceGuid") String guid, @PathParam("id") long commandId,
                           @JsonPolicyApply(Policy.REST_COMMAND_UPDATE_FROM_DEVICE) DeviceCommandUpdate command,
                           @Context SecurityContext securityContext) {

        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        logger.debug("Device command update requested. deviceId = {} commandId = {}", guid, commandId);
        User user = principal.getUser();
        if (user == null && principal.getKey() != null) {
            user = principal.getKey().getUser();
        }
        Device device = deviceService.getDevice(guid, user, principal.getDevice());
        if (command == null) {
            return ResponseFactory.response(Response.Status.NOT_FOUND,
                    new ErrorResponse("command with id " + commandId + " for device with " + guid + " is not found"));
        }
        command.setId(commandId);

        commandService.submitDeviceCommandUpdate(command, device);
        logger.debug("Device command update proceed successfully deviceId = {} commandId = {}", guid, commandId);

        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }

    @PreDestroy
    public void shutdownThreads() {
        logger.debug("Try to shutdown device commands' pool");
        asyncPool.shutdown();
        logger.debug("Device commands' pool has been shut down");
    }

    @PostConstruct
    public void initPool() {
        asyncPool = Executors.newCachedThreadPool();
    }
}
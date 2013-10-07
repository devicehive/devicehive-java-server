package com.devicehive.controller;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.messages.handler.RestHandlerCreator;
import com.devicehive.messages.subscriptions.*;
import com.devicehive.model.*;
import com.devicehive.model.response.CommandPollManyResponse;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.service.*;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.utils.SortOrder;
import com.devicehive.utils.ThreadLocalVariablesKeeper;
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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.devicehive.auth.AllowedKeyAction.Action.*;
import static javax.ws.rs.core.Response.Status.*;

/**
 * REST controller for device commands: <i>/device/{deviceGuid}/command</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceCommand">DeviceHive RESTful API: DeviceCommand</a> for details.
 */
@Path("/device")
@LogExecutionTime
@Singleton
public class DeviceCommandController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceCommandController.class);
    private DeviceCommandService commandService;
    private DeviceService deviceService;
    private UserService userService;
    private DeviceCommandDAO deviceCommandDAO;
    private SubscriptionManager subscriptionManager;
    private TimestampService timestampService;
    private AccessKeyService accessKeyService;
    private ExecutorService asyncPool;

    @EJB
    public void setCommandService(DeviceCommandService commandService) {
        this.commandService = commandService;
    }

    @EJB
    public void setDeviceService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @EJB
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @EJB
    public void setDeviceCommandDAO(DeviceCommandDAO deviceCommandDAO) {
        this.deviceCommandDAO = deviceCommandDAO;
    }

    @EJB
    public void setSubscriptionManager(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    @EJB
    public void setTimestampService(TimestampService timestampService) {
        this.timestampService = timestampService;
    }

    @EJB
    public void setAccessKeyService(AccessKeyService accessKeyService) {
        this.accessKeyService = accessKeyService;
    }

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
    @AllowedKeyAction(action = {GET_DEVICE_COMMAND})
    @Path("/{deviceGuid}/command/poll")
    public void poll(
            @PathParam("deviceGuid") final String deviceGuid,
            @QueryParam("timestamp") final Timestamp timestamp,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam("waitTimeout") final long timeout,
            @Suspended final AsyncResponse asyncResponse) {

        final HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                logger.debug("DeviceCommand poll proceed successfully. deviceid = {}", deviceGuid);
            }
        });
        asyncPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    pollAction(deviceGuid, timestamp, timeout, principal, asyncResponse);
                } catch (Exception e) {
                    asyncResponse.resume(e);
                }
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
            if (!accessKeyService.hasAccessToDevice(principal.getKey(), deviceGuid)) {
                logger.debug("DeviceCommand poll requested by Key = {}, deviceId = {}, " +
                        "timestamp = cannot be proceed. No permissions to access device",
                        principal.getKey().getId(), deviceGuid, timestamp);
                Response response = ResponseFactory.response(Response.Status.NOT_FOUND,
                        new ErrorResponse(Response.Status
                                .NOT_FOUND.getStatusCode(), "No accessible device found with such guid"));
                asyncResponse.resume(response);
            }

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
                    new CommandSubscription(principal, device.getId(), reqId,
                            restHandlerCreator);

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


    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    @Path("/command/poll")
    public void pollMany(
            @QueryParam("deviceGuids") final String deviceGuids,
            @QueryParam("timestamp") final Timestamp timestamp,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam("waitTimeout") final long timeout,
            @Context SecurityContext securityContext,
            @Suspended final AsyncResponse asyncResponse) {

        final User user = ((HivePrincipal) securityContext.getUserPrincipal()).getUser();
        final HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                logger.debug("Device notification poll many proceed successfully for devices: {}", deviceGuids);
            }
        });

        asyncPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    asyncResponsePollManyProcess(principal, deviceGuids, timestamp, timeout, user, asyncResponse);
                } catch (Exception e) {
                    logger.error("Error: " + e.getMessage(), e);
                    asyncResponse.resume(e);
                }
            }
        });
    }

    private void asyncResponsePollManyProcess(HivePrincipal principal,
                                              String deviceGuids,
                                              Timestamp timestamp,
                                              long timeout,
                                              User user, AsyncResponse asyncResponse) {
        logger.debug("Device notification pollMany requested for devices: {}. Timestamp: {}. Timeout = {}",
                deviceGuids, timestamp, timeout);

        List<String> guids =
                deviceGuids == null ? Collections.<String>emptyList() : Arrays.asList(deviceGuids.split(","));

        if (!checkPermissions(deviceGuids, timestamp, guids, principal, asyncResponse)) {
            return;
        }

        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }
        List<DeviceCommand> list = getDeviceCommandsList(principal, guids, timestamp);

        if (list.isEmpty()) {
            CommandSubscriptionStorage storage = subscriptionManager.getCommandSubscriptionStorage();
            String reqId = UUID.randomUUID().toString();
            RestHandlerCreator restHandlerCreator = new RestHandlerCreator();
            Set<CommandSubscription> subscriptionSet = new HashSet<>();
            if (!guids.isEmpty()) {
                List<Device> devices;

                if (user.getRole().equals(UserRole.ADMIN)) {
                    devices = deviceService.findByUUID(guids);
                } else {
                    devices = deviceService.findByUUIDListAndUser(user, guids);
                }
                for (Device device : devices) {
                    subscriptionSet
                            .add(new CommandSubscription(principal, device.getId(), reqId, restHandlerCreator));
                }
            } else {
                subscriptionSet
                        .add(new CommandSubscription(principal,
                                Constants.DEVICE_COMMAND_NULL_ID_SUBSTITUTE,
                                reqId,
                                restHandlerCreator));
            }

            if (SimpleWaiter
                    .subscribeAndWait(storage, subscriptionSet, restHandlerCreator.getFutureTask(), timeout)) {
                list = getDeviceCommandsList(principal, guids, timestamp);
            }
            List<CommandPollManyResponse> resultList = new ArrayList<>(list.size());
            for (DeviceCommand command : list) {
                resultList.add(new CommandPollManyResponse(command, command.getDevice().getGuid()));
            }

            asyncResponse
                    .resume(ResponseFactory.response(Response.Status.OK, resultList, Policy.COMMAND_LISTED));
            return;
        }
        List<CommandPollManyResponse> resultList = new ArrayList<>(list.size());
        for (DeviceCommand command : list) {
            resultList.add(new CommandPollManyResponse(command, command.getDevice().getGuid()));
        }
        asyncResponse.resume(ResponseFactory.response(Response.Status.OK, resultList, Policy.COMMAND_LISTED));
    }

    private boolean checkPermissions(String deviceGuids,
                                     Timestamp timestamp,
                                     List<String> guids,
                                     HivePrincipal principal,
                                     AsyncResponse asyncResponse) {
        if (principal.getUser() != null) {
            logger.debug("DeviceNotification poll was requested by User = {}, deviceIds = {}, timestamp = ",
                    principal.getUser().getLogin(), deviceGuids, timestamp);
        } else {
            logger.debug("DeviceNotification poll was requested by Key = {}, deviceIds = {}, timestamp = ",
                    principal.getKey().getId(), deviceGuids, timestamp);
            if (!guids.isEmpty()) {
                List<String> guidsWithDeniedAccess = new LinkedList<>();
                for (String deviceGuid : guids) {
                    if (!accessKeyService.hasAccessToDevice(principal.getKey(), deviceGuid)) {
                        logger.debug("DeviceNotification poll requested by Key = {}, deviceId = {}, " +
                                "timestamp = cannot be proceed. No permissions to access device",
                                principal.getKey().getId(), deviceGuid, timestamp);
                        guidsWithDeniedAccess.add(deviceGuid);
                    }
                }
                if (!guidsWithDeniedAccess.isEmpty()) {
                    StringBuilder message = new StringBuilder("No access to devices with guids: {");
                    for (String guid : guidsWithDeniedAccess) {
                        message.append(guid).append(", ");
                    }
                    message.delete(message.length() - 2, message.length());    //2 is a constant for string ", "
                    // with length 2. We don't need extra commas and spaces.
                    message.append("}");
                    Response response = ResponseFactory.response(UNAUTHORIZED,
                            new ErrorResponse(UNAUTHORIZED.getStatusCode(), message.toString()));
                    asyncResponse.resume(response);
                    return false;
                }
            }
        }
        return true;
    }

    private List<DeviceCommand> getDeviceCommandsList(HivePrincipal principal,
                                                                List<String> guids,
                                                                Timestamp timestamp) {
        User authUser = principal.getUser();
        if (authUser == null && principal.getKey() != null) {
            authUser = principal.getKey().getUser();
        }
        List<DeviceCommand> result;
        if (!guids.isEmpty()) {
            if (authUser != null && authUser.getRole().equals(UserRole.CLIENT)) {
                result = deviceCommandDAO.getByUserAndDevicesNewerThan(guids, timestamp, authUser);
            } else {
                result = deviceCommandDAO.getByDeviceIdNewerThan(guids, timestamp);
            }
        } else {
            if (authUser != null && authUser.getRole().equals(UserRole.CLIENT)) {
                result = deviceCommandDAO.getNewerThan(timestamp, authUser);
            } else {
                result = deviceCommandDAO.getNewerThan(timestamp);
            }
        }
        if (principal.getUser() == null && principal.getKey() != null) {
            Iterator<DeviceCommand> iterator = result.iterator();
            while (iterator.hasNext()) {
                DeviceCommand command = iterator.next();
                if (!accessKeyService.hasAccessToDevice(principal.getKey(), command.getDevice())) {
                    iterator.remove();
                }
            }
        }
        return result;
    }
    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand/wait">DeviceHive RESTful API: DeviceCommand: wait</a>
     *
     * @param timeout Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable waiting.
     * @return One of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand">DeviceCommand</a>
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE_COMMAND})
    @Path("/{deviceGuid}/command/{commandId}/poll")
    public void wait(
            @PathParam("deviceGuid") final String deviceGuid,
            @PathParam("commandId") final Long commandId,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam("waitTimeout") final long timeout,
            @Context SecurityContext securityContext,
            @Suspended final AsyncResponse asyncResponse) {

        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        final User user = principal.getUser() != null ? principal.getUser() : principal.getKey().getUser();
        if (principal.getKey() != null && !accessKeyService.hasAccessToDevice(principal.getKey(), deviceGuid)) {
            Response response = ResponseFactory.response(Response.Status.NOT_FOUND,
                    new ErrorResponse(Response.Status
                            .NOT_FOUND.getStatusCode(), "No accessible device found with such guid"));
            asyncResponse.resume(response);
        }
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
                try{
                waitAction(deviceGuid, commandId, timeout, asyncResponse, user);
                } catch (Exception e){
                    asyncResponse.resume(e);
                }
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
    @Path("/{deviceGuid}/command")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE_COMMAND})
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
        }
        device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, user, principal.getDevice());
        if (user == null && principal.getKey() != null) {
            if (!accessKeyService.hasAccessToDevice(principal.getKey(),
                    device) || !accessKeyService.hasAccessToNetwork(principal.getKey(), device.getNetwork())) {
                logger.debug("Device command query failed. Device with guid {} not found for access key", guid);
                return ResponseFactory.response(Response.Status.NOT_FOUND,
                        new ErrorResponse("Device not found"));
            }
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
    @AllowedKeyAction(action = {GET_DEVICE_COMMAND})
    @Path("/{deviceGuid}/command/{id}")
    public Response get(@PathParam("deviceGuid") String guid, @PathParam("id") long id,
                        @Context SecurityContext securityContext) {
        logger.debug("Device command get requested. deviceId = {}, commandId = {}", guid, id);

        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        User user = principal.getUser();
        if (user == null && principal.getKey() != null) {
            user = principal.getKey().getUser();
            if (!accessKeyService.hasAccessToDevice(principal.getKey(), guid)) {
                logger.debug("No permissions to access device with guid {} for access key with id {}", guid,
                        principal.getKey().getId());
                return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(), "Device with such guid not found"));
            }
        }
        Device device = deviceService.getDevice(guid, user, principal.getDevice());
        DeviceCommand result = commandService.getByGuidAndId(device.getGuid(), id);

        if (result == null) {
            logger.debug("Device command get failed. No command with id = {} found for device with guid = {}", id,
                    guid);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(), "Command Not Found"));
        }

        if (result.getUser() != null) {
            result.setUserId(result.getUser().getId());
        }

        logger.debug("Device command get proceed successfully deviceId = {} commandId = {}", guid, id);
        return ResponseFactory.response(OK, result, Policy.COMMAND_TO_DEVICE);
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
    @Path("/{deviceGuid}/command")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = {CREATE_DEVICE_COMMAND})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insert(@PathParam("deviceGuid") String guid,
                           @JsonPolicyApply(Policy.COMMAND_FROM_CLIENT) DeviceCommand deviceCommand,
                           @Context SecurityContext securityContext) {
        logger.debug("Device command insert requested. deviceId = {}, command = {}", guid, deviceCommand.getCommand());
        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        User user = principal.getUser() != null ? principal.getUser() : principal.getKey().getUser();
        if (principal.getKey() != null && !accessKeyService.hasAccessToDevice(principal.getKey(), guid)) {
            logger.debug("No permissions to access device with guid {} for access key with id {}", guid,
                    principal.getKey().getId());
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(), "Device with such guid not found"));
        }
        String login = user.getLogin();

        if (login == null) {
            return ResponseFactory.response(FORBIDDEN);
        }

        User u = userService.findUserWithNetworksByLogin(login);

        Device device = deviceService.getDevice(guid, user, principal.getDevice());

        commandService.submitDeviceCommand(deviceCommand, device, u, null);
        deviceCommand.setUserId(u.getId());

        logger.debug("Device command insertAll proceed successfully. deviceId = {} commandId = {}", guid,
                deviceCommand.getId());
        return ResponseFactory.response(CREATED, deviceCommand, Policy.COMMAND_TO_CLIENT);
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
    @Path("/{deviceGuid}/command/{id}")
    @RolesAllowed({HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.KEY})
    @AllowedKeyAction(action = {UPDATE_DEVICE_COMMAND})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("deviceGuid") String guid, @PathParam("id") long commandId,
                           @JsonPolicyApply(Policy.REST_COMMAND_UPDATE_FROM_DEVICE) DeviceCommandUpdate command,
                           @Context SecurityContext securityContext) {

        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        logger.debug("Device command update requested. deviceId = {} commandId = {}", guid, commandId);
        User user = principal.getUser();
        if (user == null && principal.getKey() != null) {
            user = principal.getKey().getUser();
            if (!accessKeyService.hasAccessToDevice(principal.getKey(), guid)) {
                logger.debug("No permissions to access device with guid {} for access key with id {}", guid,
                        principal.getKey().getId());
                return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(), "Device with such guid not found"));
            }
        }
        Device device = deviceService.getDevice(guid, user, principal.getDevice());
        if (command == null) {
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(),"command with id " + commandId + " for device with "
                            + guid + " is not found"));
        }
        command.setId(commandId);

        commandService.submitDeviceCommandUpdate(command, device);
        logger.debug("Device command update proceed successfully deviceId = {} commandId = {}", guid, commandId);

        return ResponseFactory.response(NO_CONTENT);
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
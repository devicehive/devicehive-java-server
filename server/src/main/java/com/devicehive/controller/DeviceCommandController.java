package com.devicehive.controller;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.controller.converters.TimestampQueryParamParser;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.controller.util.SimpleWaiter;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.messages.handler.RestHandlerCreator;
import com.devicehive.messages.subscriptions.*;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.User;
import com.devicehive.model.response.CommandPollManyResponse;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ParseUtil;
import com.google.common.util.concurrent.Runnables;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.FutureTask;

import static com.devicehive.auth.AllowedKeyAction.Action.*;
import static com.devicehive.configuration.Constants.*;
import static javax.ws.rs.core.Response.Status.*;

/**
 * REST controller for device commands: <i>/device/{deviceGuid}/command</i>. See <a
 * href="http://www.devicehive.com/restful#Reference/DeviceCommand">DeviceHive RESTful API: DeviceCommand</a> for
 * details.
 */
@Path("/device")
@LogExecutionTime
public class DeviceCommandController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceCommandController.class);

    @EJB
    private DeviceCommandService commandService;
    @EJB
    private DeviceService deviceService;
    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private TimestampService timestampService;
    @Resource(name = "concurrent/DeviceHiveWaitService")
    private ManagedExecutorService mes;
    @Inject
    private HiveSecurityContext hiveSecurityContext;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand/poll">DeviceHive RESTful
     * API: DeviceCommand: poll</a>
     *
     * @param deviceGuid Device unique identifier.
     * @param timestamp  Timestamp of the last received command (UTC). If not specified, the server's timestamp is taken
     *                   instead.
     * @param timeout    Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable
     *                   waiting.
     */
    @GET
    @Path("/{deviceGuid}/command/poll")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_COMMAND)
    public void poll(
            @PathParam(DEVICE_GUID) final String deviceGuid,
            @QueryParam(NAMES) final String namesString,
            @QueryParam(TIMESTAMP) final String timestamp,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam(WAIT_TIMEOUT) final long timeout,
            @Suspended final AsyncResponse asyncResponse) {
        poll(timeout, deviceGuid, namesString, timestamp, asyncResponse, false);
    }

    @GET
    @Path("/command/poll")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_COMMAND)
    public void pollMany(
            @QueryParam(DEVICE_GUIDS) String deviceGuidsString,
            @QueryParam(NAMES) final String namesString,
            @QueryParam(TIMESTAMP) final String timestamp,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam(WAIT_TIMEOUT) final long timeout,
            @Suspended final AsyncResponse asyncResponse) {
        poll(timeout, deviceGuidsString, namesString, timestamp, asyncResponse, true);
    }

    private void poll(final long timeout,
                      final String deviceGuids,
                      final String namesString,
                      final String timestamp,
                      final AsyncResponse asyncResponse,
                      final boolean isMany) {
        final HivePrincipal principal = hiveSecurityContext.getHivePrincipal();

        final Timestamp ts = TimestampQueryParamParser.parse(timestamp);

        final String devices = StringUtils.isNoneBlank(deviceGuids) ? deviceGuids : null;
        final String names = StringUtils.isNoneBlank(namesString) ? namesString : null;

        mes.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    getOrWaitForCommands(principal, devices, names, ts, timeout, asyncResponse, isMany);
                } catch (Exception e) {
                    asyncResponse.resume(e);
                }
            }
        });
    }

    private void getOrWaitForCommands(HivePrincipal principal, final String devices, final String names, Timestamp timestamp,
                                      long timeout, final AsyncResponse asyncResponse, final boolean isMany) {
        LOGGER.debug("Device command pollMany requested for : {}, {}, {}, {}.  Timeout = {}", devices, names, timestamp,
                timeout);

        if (timeout <= 0) {
            commandService.submitEmptyResponse(asyncResponse);
        }

        final List<String> deviceGuids = ParseUtil.getList(devices);
        final List<String> commandNames = ParseUtil.getList(names);
        Collection<DeviceCommand> list = new ArrayList<>();

        if (timestamp != null) {
            list = commandService.getDeviceCommandsList(deviceGuids, commandNames, timestamp, null, false, principal);
        }

        if (!list.isEmpty()) {
            Response response;
            if (isMany) {
                List<CommandPollManyResponse> resultList = new ArrayList<>(list.size());
                for (DeviceCommand command : list) {
                    resultList.add(new CommandPollManyResponse(command, command.getDeviceGuid()));
                }
                response = ResponseFactory.response(Response.Status.OK, resultList, Policy.COMMAND_LISTED);
            } else {
                response = ResponseFactory.response(Response.Status.OK, list, Policy.COMMAND_LISTED);
            }
            LOGGER.debug("Commands poll result: {}", response.getEntity());
            asyncResponse.resume(response);
        } else {
            CommandSubscriptionStorage storage = subscriptionManager.getCommandSubscriptionStorage();
            UUID reqId = UUID.randomUUID();
            Set<CommandSubscription> subscriptionSet = new HashSet<>();

            if (StringUtils.isNotEmpty(devices)) {
                List<String> availableDevices = deviceService.findGuidsWithPermissionsCheck(ParseUtil.getList(devices), principal);
                for (String guid : availableDevices) {
                    subscriptionSet.add(new CommandSubscription(principal, guid, reqId, names,
                            RestHandlerCreator.createCommandInsert(asyncResponse, isMany)));
                }
            } else {
                subscriptionSet.add(new CommandSubscription(principal, Constants.NULL_SUBSTITUTE, reqId, names,
                        RestHandlerCreator.createCommandInsert(asyncResponse, isMany)));
            }

            if (!SimpleWaiter.subscribeAndWait(storage, subscriptionSet, new FutureTask<Void>(Runnables.doNothing(), null), timeout)) {
                commandService.submitEmptyResponse(asyncResponse);
            }
        }
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand/wait">DeviceHive RESTful
     * API: DeviceCommand: wait</a>
     *
     * @param timeout Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable
     *                waiting.
     */
    @GET
    @Path("/{deviceGuid}/command/{commandId}/poll")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_COMMAND)
    public void wait(
            @PathParam(DEVICE_GUID) final String deviceGuid,
            @PathParam(COMMAND_ID) final String commandId,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam(WAIT_TIMEOUT) final long timeout,
            @Suspended final AsyncResponse asyncResponse) {

        final HivePrincipal principal = hiveSecurityContext.getHivePrincipal();

        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                LOGGER.debug("DeviceCommand poll proceed successfully: deviceGuid {}, commandId {}", deviceGuid, commandId);
            }
        });

        mes.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    waitAction(deviceGuid, commandId, timeout, asyncResponse, principal);
                } catch (Exception e) {
                    asyncResponse.resume(e);
                }
            }
        });
    }

    private void waitAction(String deviceGuid, String commandId, long timeout, AsyncResponse asyncResponse,
                            HivePrincipal principal) {
        LOGGER.debug("DeviceCommand wait requested, deviceId = {},  commandId = {}", deviceGuid, commandId);
        if (timeout <= 0) {
            asyncResponse.resume(ResponseFactory.response(Response.Status.OK, Collections.emptyList(), JsonPolicyDef.Policy.COMMAND_TO_DEVICE));
        }
        if (deviceGuid == null || commandId == null) {
            LOGGER.warn("DeviceCommand wait request failed. BAD REQUEST: deviceGuid and commandId required", deviceGuid);
            Response response = ResponseFactory.response(Response.Status.BAD_REQUEST);
            asyncResponse.resume(response);
            return;
        }

        Device device = deviceService.findByGuidWithPermissionsCheck(deviceGuid, principal);

        if (device == null) {
            LOGGER.warn("DeviceCommand wait request failed. NOT FOUND: device {} not found", deviceGuid);
            Response response = ResponseFactory.response(Response.Status.NOT_FOUND);
            asyncResponse.resume(response);
            return;
        }

        DeviceCommand command = commandService.findByIdAndGuid(Long.valueOf(commandId), device.getGuid());

        if (command == null) {
            LOGGER.warn("DeviceCommand wait request failed. NOT FOUND: No command found with id = {} for deviceId = {}",
                    commandId, deviceGuid);
            Response response = ResponseFactory.response(Response.Status.NOT_FOUND);
            asyncResponse.resume(response);
            return;
        }

        if (!command.getDeviceGuid().equals(device.getGuid())) {
            LOGGER.warn("DeviceCommand wait request failed. BAD REQUEST: Command with id = {} was not sent for device with guid = {}",
                    commandId, deviceGuid);
            Response response = ResponseFactory.response(Response.Status.BAD_REQUEST);
            asyncResponse.resume(response);
            return;
        }

        if (!command.getIsUpdated()) {
            CommandUpdateSubscriptionStorage storage = subscriptionManager.getCommandUpdateSubscriptionStorage();
            UUID reqId = UUID.randomUUID();
            CommandUpdateSubscription commandSubscription =
                    new CommandUpdateSubscription(Long.valueOf(commandId), reqId, RestHandlerCreator.createCommandUpdate(asyncResponse));

            if (!SimpleWaiter.subscribeAndWait(storage, commandSubscription, new FutureTask<Void>(Runnables.doNothing(), null), timeout)) {
                commandService.submitEmptyResponse(asyncResponse);
            }
        }

        Response result = ResponseFactory.response(Response.Status.OK, command, Policy.COMMAND_TO_DEVICE);
        asyncResponse.resume(result);

    }

    @GET
    @Path("/{deviceGuid}/command")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_COMMAND)
    public Response query(@PathParam(DEVICE_GUID) String guid,
                          @QueryParam(START) String startTs,
                          @QueryParam(END) String endTs,
                          @QueryParam(COMMAND) String command,
                          @QueryParam(STATUS) String status,
                          @QueryParam(SORT_FIELD) @DefaultValue(TIMESTAMP) String sortField,
                          @QueryParam(SORT_ORDER) String sortOrderSt,
                          @QueryParam(TAKE) Integer take,
                          @QueryParam(SKIP) Integer skip,
                          @QueryParam(GRID_INTERVAL) Integer gridInterval) {

        LOGGER.debug("Device command query requested for device {}", guid);

        final HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        final Timestamp timestamp = TimestampQueryParamParser.parse(startTs);

        deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);

        final Collection<DeviceCommand> commandList = commandService.getDeviceCommandsList(Arrays.asList(guid),
                StringUtils.isNoneEmpty(command) ? Arrays.asList(command) : null, timestamp, status, true, principal);

        LOGGER.debug("Device command query request proceed successfully for device {}", guid);
        return ResponseFactory.response(Response.Status.OK, commandList, Policy.COMMAND_LISTED);
    }

    /**
     * Response contains following output: <p/> <code> { "id":    1 "timestamp":     "1970-01-01 00:00:00.0" "userId": 1
     * "command":   "command_name" "parameters":    {/ * JSON Object * /} "lifetime":  100 "flags":     1 "status":
     * "comand_status" "result":    { / * JSON Object* /} } </code>
     *
     * @param guid String with Device GUID like "550e8400-e29b-41d4-a716-446655440000"
     * @param commandId   command id
     */
    @GET
    @Path("/{deviceGuid}/command/{commandId}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_COMMAND)
    public Response get(@PathParam(DEVICE_GUID) String guid, @PathParam(COMMAND_ID) String commandId) {
        LOGGER.debug("Device command get requested. deviceId = {}, commandId = {}", guid, commandId);

        final HivePrincipal principal = hiveSecurityContext.getHivePrincipal();

        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                            String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }

        DeviceCommand command = commandService.findByIdAndGuid(Long.valueOf(commandId), device.getGuid());
        if (command == null) {
            LOGGER.warn("Device command get failed. No command with id = {} found for device with guid = {}", commandId, guid);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.COMMAND_NOT_FOUND, commandId)));
        }

        if (!command.getDeviceGuid().equals(guid)) {
            LOGGER.debug("DeviceCommand wait request failed. Command with id = {} was not sent for device with guid = {}",
                    commandId, guid);
            return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(),
                    String.format(Messages.COMMAND_NOT_FOUND, commandId)));
        }

        LOGGER.debug("Device command get proceed successfully deviceId = {} commandId = {}", guid, commandId);
        return ResponseFactory.response(OK, command, Policy.COMMAND_TO_DEVICE);
    }

    /**
     * <b>Creates new device command.</b> <p/> <i>Example request:</i> <code> { "command":   "command name",
     * "parameters":    {/ * Custom Json Object * /}, "lifetime": 0, "flags": 0 } </code> <p> Where, command  is Command
     * name, required parameters   Command parameters, a JSON object with an arbitrary structure. is not required
     * lifetime     Command lifetime, a number of seconds until this command expires. is not required flags    Command
     * flags, and optional value that could be supplied for device or related infrastructure. is not required\ </p> <p>
     * <i>Example response:</i> </p> <code> { "id": 1, "timestamp": "1970-01-01 00:00:00.0", "userId":    1 } </code>
     *
     * @param guid          device guid
     * @param deviceCommand device command resource
     */
    @POST
    @Path("/{deviceGuid}/command")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = CREATE_DEVICE_COMMAND)
    public Response insert(@PathParam(DEVICE_GUID) String guid,
                           @JsonPolicyApply(Policy.COMMAND_FROM_CLIENT) DeviceCommandWrapper deviceCommand) {
        LOGGER.debug("Device command insert requested. deviceId = {}, command = {}", guid, deviceCommand.getCommand());
        final HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        User authUser = principal.getUser() != null ? principal.getUser() : principal.getKey().getUser();
        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);

        if (device == null) {
            LOGGER.warn("Device command insert failed. No device with guid = {} found", guid);
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(),
                            String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }

        final DeviceCommand message = commandService.convertToDeviceCommand(deviceCommand, device, authUser, null);
        commandService.submitDeviceCommand(message);

        LOGGER.debug("Device command insertAll proceed successfully. deviceId = {} command = {}", guid,
                deviceCommand.getCommand());
        return ResponseFactory.response(CREATED, message, Policy.COMMAND_TO_CLIENT);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceCommand/update">DeviceHive RESTful
     * API: DeviceCommand: update</a> Updates an existing device command.
     *
     * @param guid      Device unique identifier.
     * @param commandId Device command identifier.
     * @param command   In the request body, supply a <a href="http://www.devicehive .com/restful#Reference/DeviceCommand">DeviceCommand</a>
     *                  resource. All fields are not required: flags - Command flags, and optional value that could be
     *                  supplied for device or related infrastructure. status - Command status, as reported by device or
     *                  related infrastructure. result - Command execution result, an optional value that could be
     *                  provided by device.
     * @return If successful, this method returns an empty response body.
     */
    @PUT
    @Path("/{deviceGuid}/command/{commandId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.KEY})
    @AllowedKeyAction(action = UPDATE_DEVICE_COMMAND)
    public Response update(@PathParam(DEVICE_GUID) String guid, @PathParam(COMMAND_ID) Long commandId,
                           @JsonPolicyApply(Policy.REST_COMMAND_UPDATE_FROM_DEVICE) DeviceCommandWrapper command) {

        final HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        final User authUser = principal.getUser() != null ? principal.getUser() :
                (principal.getKey() != null ? principal.getKey().getUser() : null);
        LOGGER.debug("Device command update requested. command {}", command);
        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            LOGGER.warn("Device command update failed. No device with guid = {} found", guid);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                            String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }
        DeviceCommand savedCommand = commandService.findByIdAndGuid(commandId, guid);
        if (savedCommand == null) {
            LOGGER.warn("Device command get failed. No command with id = {} found for device with guid = {}", commandId, guid);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                            String.format(Messages.COMMAND_NOT_FOUND, commandId)));
        }

        DeviceCommand message = commandService.convertToDeviceCommand(command, device, authUser, commandId);
        commandService.submitDeviceCommandUpdate(message);
        LOGGER.debug("Device command update proceed successfully deviceId = {} commandId = {}", guid, commandId);

        return ResponseFactory.response(NO_CONTENT);
    }

}
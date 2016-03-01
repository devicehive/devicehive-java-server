package com.devicehive.resource.impl;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.messages.handler.RestHandlerCreator;
import com.devicehive.messages.subscriptions.*;
import com.devicehive.model.*;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.resource.DeviceCommandResource;
import com.devicehive.resource.converters.TimestampQueryParamParser;
import com.devicehive.resource.util.CommandResponseFilterAndSort;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.resource.util.SimpleWaiter;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;
import com.devicehive.util.ParseUtil;
import com.google.common.util.concurrent.Runnables;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.*;

/**
 * {@inheritDoc}
 */
@Service
public class DeviceCommandResourceImpl implements DeviceCommandResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceCommandResourceImpl.class);

    @Autowired
    private DeviceCommandService commandService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private SubscriptionManager subscriptionManager;
    @Autowired
    @Qualifier(DeviceHiveApplication.MESSAGE_EXECUTOR)
    private ExecutorService mes;

    /**
     * {@inheritDoc}
     */
    @Override
    public void poll(final String deviceGuid, final String namesString, final String timestamp, long timeout, final AsyncResponse asyncResponse) {
        poll(timeout, deviceGuid, namesString, timestamp, asyncResponse, false);
    }

    @Override
    public void pollMany(String deviceGuidsString, final String namesString, final String timestamp, long timeout, final AsyncResponse asyncResponse) {
        poll(timeout, deviceGuidsString, namesString, timestamp, asyncResponse, true);
    }

    private void poll(final long timeout,
                      final String deviceGuids,
                      final String namesString,
                      final String timestamp,
                      final AsyncResponse asyncResponse,
                      final boolean isMany) {
        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        final Date ts = TimestampQueryParamParser.parse(timestamp);

        final String devices = StringUtils.isNoneBlank(deviceGuids) ? deviceGuids : null;
        final String names = StringUtils.isNoneBlank(namesString) ? namesString : null;

        mes.submit(() -> {
            try {
                getOrWaitForCommands(principal, devices, names, ts, timeout, asyncResponse, isMany);
            } catch (Exception e) {
                asyncResponse.resume(e);
            }
        });
    }

    private void getOrWaitForCommands(HivePrincipal principal, final String devices, final String names, Date timestamp,
                                      long timeout, final AsyncResponse asyncResponse, final boolean isMany) {
        LOGGER.debug("Device command pollMany requested for : {}, {}, {}, {}.  Timeout = {}", devices, names, timestamp,
                timeout);

        if (timeout < 0) {
            submitEmptyResponse(asyncResponse);
        }

        final List<String> availableDevices = (StringUtils.isNotEmpty(devices))
                ? deviceService.findGuidsWithPermissionsCheck(ParseUtil.getList(devices), principal)
                : new ArrayList<>();
        final List<String> commandNames = ParseUtil.getList(names);
        Collection<DeviceCommand> list = new ArrayList<>();
        CommandSubscriptionStorage storage = subscriptionManager.getCommandSubscriptionStorage();
        UUID reqId = UUID.randomUUID();
        Set<CommandSubscription> subscriptionSet = new HashSet<>();
        FutureTask<Void> simpleWaitTask = new FutureTask<>(Runnables.doNothing(), null);

        if (!availableDevices.isEmpty()) {
            List<CommandSubscription> commandSubscriptions = availableDevices.stream()
                    .map(guid -> getInsertSubscription(principal, guid, reqId, names, asyncResponse, isMany, simpleWaitTask))
                    .collect(Collectors.toList());
            subscriptionSet.addAll(commandSubscriptions);
        } else {
            subscriptionSet.add(getInsertSubscription(principal, Constants.NULL_SUBSTITUTE, reqId, names,
                    asyncResponse, isMany, simpleWaitTask));
        }

        if (timestamp != null && !availableDevices.isEmpty()) {
            list = commandService.find(availableDevices, commandNames, timestamp, null, Constants.DEFAULT_TAKE, false, principal);
        }

        if (!list.isEmpty()) {
            Response response = ResponseFactory.response(Response.Status.OK, list, Policy.COMMAND_LISTED);
            LOGGER.debug("Commands poll result: {}", response.getEntity());
            asyncResponse.resume(response);
        } else {
            if (!SimpleWaiter.subscribeAndWait(storage, subscriptionSet, simpleWaitTask, timeout)) {
                submitEmptyResponse(asyncResponse);
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
    @Override
    public void wait(final String deviceGuid, final String commandId, long timeout, final AsyncResponse asyncResponse) {

        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                LOGGER.debug("DeviceCommand poll proceed successfully: deviceGuid {}, commandId {}", deviceGuid, commandId);
            }
        });

        mes.submit(() -> {
            try {
                waitAction(deviceGuid, commandId, timeout, asyncResponse, principal);
            } catch (Exception e) {
                asyncResponse.resume(e);
            }
        });
    }

    private void waitAction(String deviceGuid, String commandId, long timeout, AsyncResponse asyncResponse,
                            HivePrincipal principal) {
        LOGGER.debug("DeviceCommand wait requested, deviceId = {},  commandId = {}", deviceGuid, commandId);
        if (timeout < 0) {
            asyncResponse.resume(ResponseFactory.response(Response.Status.NO_CONTENT));
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

        DeviceCommand command = commandService.find(Long.valueOf(commandId), device.getGuid());

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
                asyncResponse.resume(ResponseFactory.response(Response.Status.NO_CONTENT));
            }
        }

        Response result = ResponseFactory.response(Response.Status.OK, command, Policy.COMMAND_TO_DEVICE);
        asyncResponse.resume(result);

    }

    @Override
    public Response query(String guid, String startTs, String endTs, String command, String status, String sortField,
                          String sortOrderSt, Integer take, Integer skip, Integer gridInterval) {
        LOGGER.debug("Device command query requested for device {}", guid);

        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final Date timestamp = TimestampQueryParamParser.parse(startTs);

        deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);

        List<String> searchCommands = StringUtils.isNoneEmpty(command) ? Collections.singletonList(command) : null;

        final Collection<DeviceCommand> commandList = commandService.find(Collections.singletonList(guid),
                searchCommands, timestamp, status, 0, null, principal);

        final Comparator<DeviceCommand> comparator = CommandResponseFilterAndSort.buildDeviceCommandComparator(sortField);
        final Boolean reverse = sortOrderSt == null ? null : "desc".equalsIgnoreCase(sortOrderSt);

        final List<DeviceCommand> sortedDeviceCommands = CommandResponseFilterAndSort.orderAndLimit(new ArrayList<>(commandList),
                comparator, reverse, skip, take);
        LOGGER.debug("Device command query request proceed successfully for device {}", guid);
        return ResponseFactory.response(Response.Status.OK, sortedDeviceCommands, Policy.COMMAND_LISTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response get(String guid, String commandId) {
        LOGGER.debug("Device command get requested. deviceId = {}, commandId = {}", guid, commandId);

        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                            String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }

        DeviceCommand command = commandService.find(Long.valueOf(commandId), device.getGuid());
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
     * {@inheritDoc}
     */
    @Override
    public Response insert(String guid, DeviceCommandWrapper deviceCommand) {
        LOGGER.debug("Device command insert requested. deviceId = {}, command = {}", guid, deviceCommand.getCommand());
        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User authUser = principal.getUser() != null ? principal.getUser() : principal.getKey().getUser();
        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);

        if (device == null) {
            LOGGER.warn("Device command insert failed. No device with guid = {} found", guid);
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(),
                            String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }

        final DeviceCommand command = commandService.insert(deviceCommand, device, authUser);

        LOGGER.debug("Device command insertAll proceed successfully. deviceId = {} command = {}", guid,
                deviceCommand.getCommand());
        return ResponseFactory.response(CREATED, command, Policy.COMMAND_TO_CLIENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response update(String guid, Long commandId, DeviceCommandWrapper command) {

        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final User authUser = principal.getUser() != null ? principal.getUser() :
                (principal.getKey() != null ? principal.getKey().getUser() : null);
        LOGGER.debug("Device command update requested. command {}", command);
        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            LOGGER.warn("Device command update failed. No device with guid = {} found", guid);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                            String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }
        DeviceCommand savedCommand = commandService.find(commandId, guid);
        if (savedCommand == null) {
            LOGGER.warn("Device command get failed. No command with id = {} found for device with guid = {}", commandId, guid);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                            String.format(Messages.COMMAND_NOT_FOUND, commandId)));
        }

        commandService.update(commandId, device.getGuid(), command);
        LOGGER.debug("Device command update proceed successfully deviceId = {} commandId = {}", guid, commandId);

        return ResponseFactory.response(NO_CONTENT);
    }

    private void submitEmptyResponse(final AsyncResponse asyncResponse) {
        asyncResponse.resume(ResponseFactory.response(Response.Status.OK, Collections.emptyList(), JsonPolicyDef.Policy.COMMAND_LISTED));
    }

    private CommandSubscription getInsertSubscription(HivePrincipal principal, String guid, UUID reqId, String names,
                                                      AsyncResponse asyncResponse, boolean isMany, FutureTask<Void> waitTask){
        return new CommandSubscription(principal, guid, reqId, names, RestHandlerCreator.createCommandInsert(asyncResponse, isMany, waitTask));
    }

}

package com.devicehive.resource.impl;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.resource.DeviceCommandResource;
import com.devicehive.resource.converters.TimestampQueryParamParser;
import com.devicehive.resource.util.CommandResponseFilterAndSort;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.UserVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
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
    @Qualifier(DeviceHiveApplication.MESSAGE_EXECUTOR)
    private ExecutorService mes;

    /**
     * {@inheritDoc}
     */
    @Override
    public void poll(final String deviceGuid, final String namesString, final String timestamp, long timeout, final AsyncResponse asyncResponse) throws Exception {
        poll(timeout, deviceGuid, namesString, timestamp, asyncResponse);
    }

    @Override
    public void pollMany(String deviceGuidsString, final String namesString, final String timestamp, long timeout, final AsyncResponse asyncResponse) throws Exception {
        poll(timeout, deviceGuidsString, namesString, timestamp, asyncResponse);
    }

    private void poll(final long timeout,
                      final String deviceGuidsCsv,
                      final String namesCsv,
                      final String timestamp,
                      final AsyncResponse asyncResponse) throws InterruptedException {
        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        final Date ts = TimestampQueryParamParser.parse(timestamp);

        if (timeout < 0) {
            asyncResponse.resume(ResponseFactory.response(
                    Response.Status.OK,
                    Collections.emptyList(),
                    JsonPolicyDef.Policy.COMMAND_LISTED));
        }
        asyncResponse.setTimeout(timeout, TimeUnit.SECONDS);

        Set<String> availableDevices = Optional.ofNullable(StringUtils.split(deviceGuidsCsv, ','))
                .map(Arrays::asList)
                .map(list -> deviceService.findByGuidWithPermissionsCheck(list, principal))
                .map(list -> list.stream().map(DeviceVO::getGuid).collect(Collectors.toSet()))
                .orElse(Collections.emptySet());

        Set<String> names = Optional.of(StringUtils.split(namesCsv, ','))
                .map(Arrays::asList)
                .map(list -> list.stream().collect(Collectors.toSet()))
                .orElse(Collections.emptySet());

        BiConsumer<DeviceCommand, String> callback = (notification, subscriptionId) -> {
            if (!asyncResponse.isDone()) {
                asyncResponse.resume(ResponseFactory.response(
                        Response.Status.OK,
                        notification,
                        JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT));
            }
        };

        Pair<String, CompletableFuture<List<DeviceCommand>>> pair = commandService
                .submitCommandSubscribe(availableDevices, names, ts, callback);

        pair.getRight().thenAccept(collection -> {
            if (!collection.isEmpty() && !asyncResponse.isDone()) {
                asyncResponse.resume(ResponseFactory.response(
                        Response.Status.OK,
                        collection,
                        JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT));
            }
        }).exceptionally(throwable -> {
            if (!asyncResponse.isDone()) {
                asyncResponse.resume(throwable);
            }
            return null;
        });

        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                commandService.submitCommandUnsubscribe(pair.getLeft(), null);
            }
        });
    }

//    private void getOrWaitForCommands(HivePrincipal principal, final String devices, final String names, Date timestamp,
//                                      long timeout, final AsyncResponse asyncResponse, final boolean isMany) {
//        LOGGER.debug("Device command pollMany requested for : {}, {}, {}, {}.  Timeout = {}", devices, names, timestamp,
//                timeout);
//
//        if (timeout < 0) {
//            submitEmptyResponse(asyncResponse);
//        }
//
//        final Set<String> availableDevices = StringUtils.isBlank(devices)
//                ? Collections.emptySet()
//                : deviceService.findByGuidWithPermissionsCheck(ParseUtil.getList(devices), principal).stream()
//                    .map(DeviceVO::getGuid)
//                    .collect(Collectors.toSet());
//
//        final List<String> commandNames = ParseUtil.getList(names);
//        Collection<DeviceCommand> list = new ArrayList<>();
//        CommandSubscriptionStorage storage = subscriptionManager.getCommandSubscriptionStorage();
//        UUID reqId = UUID.randomUUID();
//        Set<CommandSubscription> subscriptionSet = new HashSet<>();
//        FutureTask<Void> simpleWaitTask = new FutureTask<>(Runnables.doNothing(), null);
//
//        if (!availableDevices.isEmpty()) {
//            List<CommandSubscription> commandSubscriptions = availableDevices.stream()
//                    .map(guid -> getInsertSubscription(principal, guid, reqId, names, asyncResponse, isMany, simpleWaitTask))
//                    .collect(Collectors.toList());
//            subscriptionSet.addAll(commandSubscriptions);
//        } else {
//            subscriptionSet.add(getInsertSubscription(principal, Constants.NULL_SUBSTITUTE, reqId, names,
//                    asyncResponse, isMany, simpleWaitTask));
//        }
//
//        if (timestamp != null && !availableDevices.isEmpty()) {
//            list = commandService
//                    .find(availableDevices, commandNames, timestamp, null, Constants.DEFAULT_TAKE, false)
//                    .join();
//        }
//
//        if (!list.isEmpty()) {
//            Response response = ResponseFactory.response(Response.Status.OK, list, Policy.COMMAND_LISTED);
//            LOGGER.debug("Commands poll result: {}", response.getEntity());
//            asyncResponse.resume(response);
//        } else {
//            if (!SimpleWaiter.subscribeAndWait(storage, subscriptionSet, simpleWaitTask, timeout)) {
//                submitEmptyResponse(asyncResponse);
//            }
//        }
//    }

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

        DeviceVO device = deviceService.findByGuidWithPermissionsCheck(deviceGuid, principal);

        if (device == null) {
            LOGGER.warn("DeviceCommand wait request failed. NOT FOUND: device {} not found", deviceGuid);
            Response response = ResponseFactory.response(Response.Status.NOT_FOUND);
            asyncResponse.resume(response);
            return;
        }

        Optional<DeviceCommand> command = commandService.find(Long.valueOf(commandId), device.getGuid()).join();

        if (!command.isPresent()) {
            LOGGER.warn("DeviceCommand wait request failed. NOT FOUND: No command found with id = {} for deviceId = {}",
                    commandId, deviceGuid);
            Response response = ResponseFactory.response(Response.Status.NOT_FOUND);
            asyncResponse.resume(response);
            return;
        }

        if (!command.get().getDeviceGuid().equals(device.getGuid())) {
            LOGGER.warn("DeviceCommand wait request failed. BAD REQUEST: Command with id = {} was not sent for device with guid = {}",
                    commandId, deviceGuid);
            Response response = ResponseFactory.response(Response.Status.BAD_REQUEST);
            asyncResponse.resume(response);
            return;
        }

        if (!command.get().getIsUpdated()) { // FIXME
//            CommandUpdateSubscriptionStorage storage = subscriptionManager.getCommandUpdateSubscriptionStorage();
//            UUID reqId = UUID.randomUUID();
//            CommandUpdateSubscription commandSubscription =
//                    new CommandUpdateSubscription(Long.valueOf(commandId), reqId, RestHandlerCreator.createCommandUpdate(asyncResponse));
//
//            if (!SimpleWaiter.subscribeAndWait(storage, commandSubscription, new FutureTask<>(Runnables.doNothing(), null), timeout)) {
//                asyncResponse.resume(ResponseFactory.response(Response.Status.NO_CONTENT));
//            }
        }

        Response result = ResponseFactory.response(Response.Status.OK, command, Policy.COMMAND_TO_DEVICE);
        asyncResponse.resume(result);

    }

    @Override
    public void query(String guid, String startTs, String endTs, String command, String status, String sortField,
                      String sortOrderSt, Integer take, Integer skip,  @Suspended final AsyncResponse asyncResponse) {
        LOGGER.debug("Device command query requested for device {}", guid);

        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final Date timestampSt = TimestampQueryParamParser.parse(startTs);
        final Date timestampEnd = TimestampQueryParamParser.parse(endTs);

        DeviceVO device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            ErrorResponse errorCode = new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.DEVICE_NOT_FOUND, guid));
            Response response = ResponseFactory.response(NOT_FOUND, errorCode);
            asyncResponse.resume(response);
        } else {
            List<String> searchCommands = StringUtils.isNoneEmpty(command) ? Collections.singletonList(command) : Collections.EMPTY_LIST;
            commandService.find(Collections.singletonList(guid), searchCommands, timestampSt, timestampEnd, status)
                    .thenApply(commands -> {
                        final Comparator<DeviceCommand> comparator = CommandResponseFilterAndSort.buildDeviceCommandComparator(sortField);
                        final Boolean reverse = sortOrderSt == null ? null : "desc".equalsIgnoreCase(sortOrderSt);

                        final List<DeviceCommand> sortedDeviceCommands = CommandResponseFilterAndSort.orderAndLimit(new ArrayList<>(commands),
                                comparator, reverse, skip, take);
                        return ResponseFactory.response(OK, sortedDeviceCommands, Policy.COMMAND_LISTED);
                    })
                    .thenAccept(asyncResponse::resume);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get(String guid, String commandId, @Suspended final AsyncResponse asyncResponse) {
        LOGGER.debug("Device command get requested. deviceId = {}, commandId = {}", guid, commandId);

        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        DeviceVO device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            Response response = ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.DEVICE_NOT_FOUND, guid)));
            asyncResponse.resume(response);
            return;
        }

        commandService.find(Long.valueOf(commandId), device.getGuid())
                .thenApply(command -> {
                    if (!command.isPresent()) {
                        LOGGER.warn("Device command get failed. No command with id = {} found for device with guid = {}", commandId, guid);
                        return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                                String.format(Messages.COMMAND_NOT_FOUND, commandId)));
                    }

                    if (!command.get().getDeviceGuid().equals(guid)) {
                        LOGGER.debug("DeviceCommand wait request failed. Command with id = {} was not sent for device with guid = {}",
                                commandId, guid);
                        return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(),
                                String.format(Messages.COMMAND_NOT_FOUND, commandId)));
                    }

                    LOGGER.debug("Device command get proceed successfully deviceId = {} commandId = {}", guid, commandId);
                    return ResponseFactory.response(OK, command.get(), Policy.COMMAND_TO_DEVICE);
                })
                .thenAccept(asyncResponse::resume);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(String guid, DeviceCommandWrapper deviceCommand, @Suspended final AsyncResponse asyncResponse) {
        LOGGER.debug("Device command insert requested. deviceId = {}, command = {}", guid, deviceCommand.getCommand());
        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserVO authUser = principal.getUser() != null ? principal.getUser() : principal.getKey().getUser();
        DeviceVO device = deviceService.findByGuidWithPermissionsCheck(guid, principal);

        if (device == null) {
            LOGGER.warn("Device command insert failed. No device with guid = {} found", guid);
            ErrorResponse errorCode = new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.DEVICE_NOT_FOUND, guid));
            Response response = ResponseFactory.response(NOT_FOUND, errorCode);
            asyncResponse.resume(response);
        } else {
            DeviceCommand command = commandService.insert(deviceCommand, device, authUser).join();
            if (command != null) {
                LOGGER.debug("Device command insertAll proceed successfully. deviceId = {} command = {}", guid,
                        deviceCommand.getCommand());
                Response jaxResponse = ResponseFactory.response(Response.Status.CREATED, command, Policy.COMMAND_TO_CLIENT);
                asyncResponse.resume(jaxResponse);
            } else {
                LOGGER.warn("Device command insert failed for device with guid = {}.", guid);
                ErrorResponse errorCode = new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.COMMAND_NOT_FOUND, -1L));
                Response jaxResponse = ResponseFactory.response(NOT_FOUND, errorCode);
                asyncResponse.resume(jaxResponse);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(String guid, Long commandId, DeviceCommandWrapper command, @Suspended final AsyncResponse asyncResponse) {

        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LOGGER.debug("Device command update requested. command {}", command);
        DeviceVO device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            LOGGER.warn("Device command update failed. No device with guid = {} found", guid);
            ErrorResponse errorCode = new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.DEVICE_NOT_FOUND, guid));
            Response response = ResponseFactory.response(NOT_FOUND, errorCode);
            asyncResponse.resume(response);
        } else {
            Optional<DeviceCommand> savedCommand = commandService.find(commandId, guid).join();
            if (!savedCommand.isPresent()) {
                LOGGER.warn("Device command update failed. No command with id = {} found for device with guid = {}", commandId, guid);
                Response response = ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                        String.format(Messages.COMMAND_NOT_FOUND, commandId)));
                asyncResponse.resume(response);
            } else {
                LOGGER.debug("Device command update proceed successfully deviceId = {} commandId = {}", guid, commandId);
                commandService.update(savedCommand.get(), command);
                asyncResponse.resume(ResponseFactory.response(Response.Status.NO_CONTENT));
            }
        }
    }

//    private CommandSubscription getInsertSubscription(HivePrincipal principal, String guid, UUID reqId, String names,
//                                                      AsyncResponse asyncResponse, boolean isMany, FutureTask<Void> waitTask){
//        return new CommandSubscription(principal, guid, reqId, names, RestHandlerCreator.createCommandInsert(asyncResponse, isMany, waitTask));
//    }

}

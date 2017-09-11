package com.devicehive.resource.impl;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.resource.DeviceCommandResource;
import com.devicehive.model.converters.TimestampQueryParamParser;
import com.devicehive.resource.util.CommandResponseFilterAndSort;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.NetworkService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.*;

/**
 * {@inheritDoc}
 */
@Service
public class DeviceCommandResourceImpl implements DeviceCommandResource {

    private static final Logger logger = LoggerFactory.getLogger(DeviceCommandResourceImpl.class);

    private final Gson gson;
    private final DeviceCommandService commandService;
    private final DeviceService deviceService;
    private final NetworkService networkService;
    private final TimestampService timestampService;
    private final HiveValidator hiveValidator;

    @Autowired
    public DeviceCommandResourceImpl(Gson gson,
                                     DeviceCommandService commandService,
                                     DeviceService deviceService,
                                     NetworkService networkService,
                                     TimestampService timestampService,
                                     HiveValidator hiveValidator) {
        this.gson = gson;
        this.commandService = commandService;
        this.deviceService = deviceService;
        this.networkService = networkService;
        this.timestampService = timestampService;
        this.hiveValidator = hiveValidator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void poll(final String deviceId, final String namesString, final String timestamp,
            boolean returnUpdatedCommands, final long timeout, final int limit, final AsyncResponse asyncResponse)
            throws Exception {
        poll(timeout, deviceId, null, namesString, timestamp, returnUpdatedCommands, limit, asyncResponse);
    }

    @Override
    public void pollMany(final String deviceIdsString, final String networkIdsString, final String namesString,
            final String timestamp, final long timeout, final int limit, final AsyncResponse asyncResponse)
            throws Exception {
        poll(timeout, deviceIdsString, networkIdsString, namesString, timestamp, false, limit, asyncResponse);
    }

    private void poll(final long timeout,
                      final String deviceIdsCsv,
                      final String networkIdsCsv,
                      final String namesCsv,
                      final String timestamp,
                      final boolean returnUpdated,
                      final Integer limit,
                      final AsyncResponse asyncResponse) throws InterruptedException {
        final HiveAuthentication authentication = (HiveAuthentication) SecurityContextHolder.getContext().getAuthentication();
        final HivePrincipal principal = (HivePrincipal) authentication.getPrincipal();

        final Date ts = Optional.ofNullable(timestamp).map(TimestampQueryParamParser::parse)
                .orElse(timestampService.getDate());

        final Response response = ResponseFactory.response(
                Response.Status.OK,
                Collections.emptyList(),
                Policy.COMMAND_LISTED);

        asyncResponse.setTimeoutHandler(asyncRes -> asyncRes.resume(response));

        Set<String> availableDevices = new HashSet<>();
        if (deviceIdsCsv != null) {
            availableDevices = Optional.ofNullable(StringUtils.split(deviceIdsCsv, ','))
                    .map(Arrays::asList)
                    .map(list -> deviceService.findByIdWithPermissionsCheck(list, principal))
                    .map(list -> list.stream().map(DeviceVO::getDeviceId).collect(Collectors.toSet()))
                    .orElse(Collections.emptySet());
        }
        if (networkIdsCsv != null) {
            Set<String> networkDevices = Optional.ofNullable(StringUtils.split(networkIdsCsv, ','))
                    .map(Arrays::asList)
                    .map(list -> list.stream()
                            .map(n -> gson.fromJson(n, Long.class))
                            .map(network -> networkService.getWithDevices(network, authentication))
                            .filter(Objects::nonNull).map(NetworkWithUsersAndDevicesVO::getDevices)
                            .flatMap(Collection::stream)
                            .map(DeviceVO::getDeviceId)
                            .collect(Collectors.toSet())
                    ).orElse(Collections.emptySet());
            availableDevices.addAll(networkDevices);
        }
        if (availableDevices.isEmpty()) {
            availableDevices = deviceService.findByIdWithPermissionsCheck(Collections.emptyList(), principal)
                    .stream()
                    .map(DeviceVO::getDeviceId)
                    .collect(Collectors.toSet());

        }

        Set<String> names = Optional.ofNullable(StringUtils.split(namesCsv, ','))
                .map(Arrays::asList)
                .map(list -> list.stream().collect(Collectors.toSet()))
                .orElse(Collections.emptySet());

        BiConsumer<DeviceCommand, Long> callback = (command, subscriptionId) -> {
            if (!asyncResponse.isDone()) {
                asyncResponse.resume(ResponseFactory.response(
                        Response.Status.OK,
                        Collections.singleton(command),
                        Policy.COMMAND_LISTED));
            }
        };

        Filter filter = new Filter();
        filter.setNames(names);
        if (!availableDevices.isEmpty()) {
            Pair<Long, CompletableFuture<List<DeviceCommand>>> pair = commandService
                    .sendSubscribeRequest(availableDevices, filter, ts, returnUpdated, limit, callback);
            pair.getRight().thenAccept(collection -> {
                if (!collection.isEmpty() && !asyncResponse.isDone()) {
                    asyncResponse.resume(ResponseFactory.response(
                            Response.Status.OK,
                            collection,
                            Policy.COMMAND_LISTED));
                }

                if (timeout == 0) {
                    asyncResponse.setTimeout(1, TimeUnit.MILLISECONDS); // setting timeout to 0 would cause
                    // the thread to suspend indefinitely, see AsyncResponse docs
                } else {
                    asyncResponse.setTimeout(timeout, TimeUnit.SECONDS);
                }
            });

            asyncResponse.register((CompletionCallback) throwable -> commandService.sendUnsubscribeRequest(pair.getLeft(), null));
        } else {
            if (!asyncResponse.isDone()) {
                asyncResponse.resume(response);
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
    public void wait(final String deviceId, final String commandId, final long timeout, final AsyncResponse asyncResponse) {

        logger.debug("DeviceCommand wait requested, deviceId = {},  commandId = {}", deviceId, commandId);

        asyncResponse.setTimeoutHandler(asyncRes ->
                asyncRes.resume(ResponseFactory.response(Response.Status.NO_CONTENT)));

        if (deviceId == null || commandId == null) {
            logger.warn("DeviceCommand wait request failed. BAD REQUEST: deviceId and commandId required", deviceId);
            asyncResponse.resume(ResponseFactory.response(Response.Status.BAD_REQUEST));
            return;
        }

        DeviceVO device = deviceService.findById(deviceId);

        if (device == null) {
            logger.warn("DeviceCommand wait request failed. NOT FOUND: device {} not found", deviceId);
            asyncResponse.resume(ResponseFactory.response(Response.Status.NOT_FOUND));
            return;
        }

        Optional<DeviceCommand> command = commandService.findOne(Long.valueOf(commandId), device.getDeviceId()).join();

        if (!command.isPresent()) {
            logger.warn("DeviceCommand wait request failed. NOT FOUND: No command found with id = {} for deviceId = {}",
                    commandId, deviceId);
            asyncResponse.resume(ResponseFactory.response(Response.Status.NO_CONTENT));
            return;
        }

        if (!command.get().getDeviceId().equals(device.getDeviceId())) {
            logger.warn("DeviceCommand wait request failed. BAD REQUEST: Command with id = {} was not sent for device with id = {}",
                    commandId, deviceId);
            asyncResponse.resume(ResponseFactory.response(Response.Status.BAD_REQUEST));
            return;
        }

        BiConsumer<DeviceCommand, Long> callback = (com, subscriptionId) -> {
            if (!asyncResponse.isDone()) {
                asyncResponse.resume(ResponseFactory.response(
                        Response.Status.OK,
                        com,
                        Policy.COMMAND_TO_DEVICE));
            }
        };

        if (!command.get().getIsUpdated()) {
            CompletableFuture<Pair<Long, DeviceCommand>> future = commandService
                    .sendSubscribeToUpdateRequest(Long.valueOf(commandId), deviceId, callback);
            future.thenAccept(pair -> {
                final DeviceCommand deviceCommand = pair.getRight();
                if (!asyncResponse.isDone() && deviceCommand.getIsUpdated()) {
                    asyncResponse.resume(ResponseFactory.response(
                            Response.Status.OK,
                            deviceCommand,
                            Policy.COMMAND_TO_DEVICE));
                }

                if (timeout == 0) {
                    asyncResponse.setTimeout(1, TimeUnit.MILLISECONDS); // setting timeout to 0 would cause
                    // the thread to suspend indefinitely, see AsyncResponse docs
                } else {
                    asyncResponse.setTimeout(timeout, TimeUnit.SECONDS);
                }
            });
            asyncResponse.register((CompletionCallback) throwable -> {
                try {
                    commandService.sendUnsubscribeRequest(future.get().getLeft(), null);
                } catch (InterruptedException | ExecutionException e) {
                    if (!asyncResponse.isDone()) {
                        asyncResponse.resume(ResponseFactory.response(Response.Status.INTERNAL_SERVER_ERROR));
                    }
                }
            });
        } else {
            if (!asyncResponse.isDone()) {
                asyncResponse.resume(ResponseFactory.response(Response.Status.OK, command.get(),
                        Policy.COMMAND_TO_DEVICE));
            }
        }

    }

    @Override
    public void query(String deviceId, String startTs, String endTs, String command, String status, String sortField,
                      String sortOrderSt, Integer take, Integer skip, @Suspended final AsyncResponse asyncResponse) {
        logger.debug("Device command query requested for device {}", deviceId);

        final Date timestampSt = TimestampQueryParamParser.parse(startTs);
        final Date timestampEnd = TimestampQueryParamParser.parse(endTs);

        DeviceVO device = deviceService.findById(deviceId);
        if (device == null) {
            ErrorResponse errorCode = new ErrorResponse(NOT_FOUND.getStatusCode(), 
                    String.format(Messages.DEVICE_NOT_FOUND, deviceId));
            Response response = ResponseFactory.response(NOT_FOUND, errorCode);
            asyncResponse.resume(response);
        } else {
            List<String> searchCommands = StringUtils.isNoneEmpty(command) ? Collections.singletonList(command) : Collections.EMPTY_LIST;
            commandService.find(Collections.singletonList(deviceId), searchCommands, timestampSt, timestampEnd, status)
                    .thenApply(commands -> {
                        final Comparator<DeviceCommand> comparator = CommandResponseFilterAndSort
                                .buildDeviceCommandComparator(sortField);
                        final Boolean reverse = sortOrderSt == null ? null : "desc".equalsIgnoreCase(sortOrderSt);

                        final List<DeviceCommand> sortedDeviceCommands = CommandResponseFilterAndSort
                                .orderAndLimit(new ArrayList<>(commands),
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
    public void get(String deviceId, String commandId, @Suspended final AsyncResponse asyncResponse) {
        logger.debug("Device command get requested. deviceId = {}, commandId = {}", deviceId, commandId);

        DeviceVO device = deviceService.findById(deviceId);
        if (device == null) {
            Response response = ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.DEVICE_NOT_FOUND, deviceId)));
            asyncResponse.resume(response);
            return;
        }

        commandService.findOne(Long.valueOf(commandId), device.getDeviceId())
                .thenApply(command -> {
                    if (!command.isPresent()) {
                        logger.warn("Device command get failed. No command with id = {} found for device with id = {}", commandId, deviceId);
                        return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                                String.format(Messages.COMMAND_NOT_FOUND, commandId)));
                    }

                    if (!command.get().getDeviceId().equals(deviceId)) {
                        logger.debug("DeviceCommand wait request failed. Command with id = {} was not sent for device with id = {}",
                                commandId, deviceId);
                        return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(),
                                String.format(Messages.COMMAND_NOT_FOUND, commandId)));
                    }

                    logger.debug("Device command get proceed successfully deviceId = {} commandId = {}", deviceId, commandId);
                    return ResponseFactory.response(OK, command.get(), Policy.COMMAND_TO_DEVICE);
                })
                .thenAccept(asyncResponse::resume);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(String deviceId, DeviceCommandWrapper deviceCommand, @Suspended final AsyncResponse asyncResponse) {
        hiveValidator.validate(deviceCommand);
        logger.debug("Device command insert requested. deviceId = {}, command = {}", deviceId, deviceCommand);
        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserVO authUser = principal.getUser();
        DeviceVO device = deviceService.findById(deviceId);

        if (device == null) {
            logger.warn("Device command insert failed. No device with id = {} found", deviceId);
            ErrorResponse errorCode = new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.DEVICE_NOT_FOUND, deviceId));
            Response response = ResponseFactory.response(NOT_FOUND, errorCode);
            asyncResponse.resume(response);
        } else {
            DeviceCommand command = commandService.insert(deviceCommand, device, authUser).join();
            if (command != null) {
                logger.debug("Device command insertAll proceed successfully. deviceId = {} command = {}", deviceId,
                        deviceCommand.getCommand());
                Response jaxResponse = ResponseFactory.response(Response.Status.CREATED, command, Policy.COMMAND_TO_CLIENT);
                asyncResponse.resume(jaxResponse);
            } else {
                logger.warn("Device command insert failed for device with id = {}.", deviceId);
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
    public void update(String deviceId, Long commandId, DeviceCommandWrapper command, @Suspended final AsyncResponse asyncResponse) {

        logger.debug("Device command update requested. command {}", command);
        DeviceVO device = deviceService.findById(deviceId);
        if (device == null) {
            logger.warn("Device command update failed. No device with id = {} found", deviceId);
            ErrorResponse errorCode = new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.DEVICE_NOT_FOUND, deviceId));
            Response response = ResponseFactory.response(NOT_FOUND, errorCode);
            asyncResponse.resume(response);
        } else {
            Optional<DeviceCommand> savedCommand = commandService.findOne(commandId, deviceId).join();
            if (!savedCommand.isPresent()) {
                logger.warn("Device command update failed. No command with id = {} found for device with id = {}", commandId, deviceId);
                Response response = ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                        String.format(Messages.COMMAND_NOT_FOUND, commandId)));
                asyncResponse.resume(response);
            } else {
                logger.debug("Device command update proceed successfully deviceId = {} commandId = {}", deviceId, commandId);
                commandService.update(savedCommand.get(), command);
                asyncResponse.resume(ResponseFactory.response(Response.Status.NO_CONTENT));
            }
        }
    }

}

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
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.resource.DeviceCommandResource;
import com.devicehive.model.converters.TimestampQueryParamParser;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.BaseFilterService;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.DeviceVO;
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

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_DEVICE;
import static com.devicehive.shim.api.Action.COMMAND_EVENT;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;

/**
 * {@inheritDoc}
 */
@Service
public class DeviceCommandResourceImpl implements DeviceCommandResource {

    private static final Logger logger = LoggerFactory.getLogger(DeviceCommandResourceImpl.class);

    private final Gson gson;
    private final DeviceCommandService commandService;
    private final DeviceService deviceService;
    private final TimestampService timestampService;
    private final BaseFilterService filterService;
    private final HiveValidator hiveValidator;

    @Autowired
    public DeviceCommandResourceImpl(Gson gson,
                                     DeviceCommandService commandService,
                                     DeviceService deviceService,
                                     TimestampService timestampService,
                                     BaseFilterService filterService,
                                     HiveValidator hiveValidator) {
        this.gson = gson;
        this.commandService = commandService;
        this.deviceService = deviceService;
        this.timestampService = timestampService;
        this.filterService = filterService;
        this.hiveValidator = hiveValidator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void poll(final String deviceId, final String namesString, final String timestamp,
            boolean returnUpdatedCommands, final long timeout, final int limit, final AsyncResponse asyncResponse)
            throws Exception {
        poll(timeout, deviceId, null, null, namesString, timestamp, returnUpdatedCommands, limit, asyncResponse);
    }

    @Override
    public void pollMany(final String deviceId, final String networkIdsString, final String deviceTypeIdsString,
            final String namesString, final String timestamp, final long timeout, final int limit, final AsyncResponse asyncResponse)
            throws Exception {
        poll(timeout, deviceId, networkIdsString, deviceTypeIdsString, namesString, timestamp, false, limit, asyncResponse);
    }

    private void poll(final long timeout,
                      final String deviceId,
                      final String networkIdsCsv,
                      final String deviceTypeIdsCsv,
                      final String namesCsv,
                      final String timestamp,
                      final boolean returnUpdated,
                      final Integer limit,
                      final AsyncResponse asyncResponse) throws InterruptedException {
        final HiveAuthentication authentication = (HiveAuthentication) SecurityContextHolder.getContext().getAuthentication();

        final Date ts = Optional.ofNullable(timestamp).map(TimestampQueryParamParser::parse)
                .orElse(timestampService.getDate());

        final Response response = ResponseFactory.response(
                OK,
                Collections.emptyList(),
                Policy.COMMAND_LISTED);

        asyncResponse.setTimeoutHandler(asyncRes -> asyncRes.resume(response));

        Set<String> names = Optional.ofNullable(StringUtils.split(namesCsv, ','))
                .map(Arrays::asList)
                .map(list -> list.stream().collect(Collectors.toSet()))
                .orElse(null);
        Set<Long> networks = Optional.ofNullable(StringUtils.split(networkIdsCsv, ','))
                .map(Arrays::asList)
                .map(list -> list.stream()
                        .map(n -> gson.fromJson(n, Long.class))
                        .collect(Collectors.toSet())
                ).orElse(null);
        Set<Long> deviceTypes = Optional.ofNullable(StringUtils.split(deviceTypeIdsCsv, ','))
                .map(Arrays::asList)
                .map(list -> list.stream()
                        .map(dt -> gson.fromJson(dt, Long.class))
                        .collect(Collectors.toSet())
                ).orElse(null);

        BiConsumer<DeviceCommand, Long> callback = (command, subscriptionId) -> {
            if (!asyncResponse.isDone()) {
                asyncResponse.resume(ResponseFactory.response(
                        OK,
                        Collections.singleton(command),
                        Policy.COMMAND_LISTED));
            }
        };

        Set<Filter> filters = filterService.getFilterList(deviceId, networks, deviceTypes, COMMAND_EVENT.name(), names, authentication);

        if (!filters.isEmpty()) {
            Pair<Long, CompletableFuture<List<DeviceCommand>>> pair = commandService
                    .sendSubscribeRequest(filters, names, ts, returnUpdated, limit, callback);
            pair.getRight().thenAccept(collection -> {
                if (!collection.isEmpty() && !asyncResponse.isDone()) {
                    asyncResponse.resume(ResponseFactory.response(
                            OK,
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

            asyncResponse.register((CompletionCallback) throwable -> commandService.sendUnsubscribeRequest(Collections.singleton(pair.getLeft())));
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
            asyncResponse.resume(ResponseFactory.response(BAD_REQUEST));
            return;
        }

        DeviceVO device = deviceService.findById(deviceId);

        if (device == null) {
            logger.warn("DeviceCommand wait request failed. NOT FOUND: device {} not found", deviceId);
            asyncResponse.resume(ResponseFactory.response(NOT_FOUND));
            return;
        }

        commandService.findOne(Long.valueOf(commandId), device.getDeviceId())
                .thenAccept(command -> {
                    if (!command.isPresent()) {
                        logger.warn("DeviceCommand wait request failed. NOT FOUND: No command found with id = {} for deviceId = {}",
                                commandId, deviceId);
                        asyncResponse.resume(ResponseFactory.response(Response.Status.NO_CONTENT));
                    } else {
                        waitForCommand(device, commandId, timeout, command.get(), asyncResponse);        
                    }
                });
    }
    
    private void waitForCommand(DeviceVO device, final String commandId, final long timeout,
            DeviceCommand command, final AsyncResponse asyncResponse) {
        String deviceId = device.getDeviceId();
        

        if (!command.getDeviceId().equals(device.getDeviceId())) {
            logger.warn("DeviceCommand wait request failed. BAD REQUEST: Command with id = {} was not sent for device with id = {}",
                    commandId, deviceId);
            asyncResponse.resume(ResponseFactory.response(BAD_REQUEST));
            return;
        }

        BiConsumer<DeviceCommand, Long> callback = (com, subscriptionId) -> {
            if (!asyncResponse.isDone()) {
                asyncResponse.resume(ResponseFactory.response(
                        OK,
                        com,
                        COMMAND_TO_DEVICE));
            }
        };

        if (!command.getIsUpdated()) {
            CompletableFuture<Pair<Long, DeviceCommand>> future = commandService
                    .sendSubscribeToUpdateRequest(Long.valueOf(commandId), device, callback);
            future.thenAccept(pair -> {
                final DeviceCommand deviceCommand = pair.getRight();
                if (!asyncResponse.isDone() && deviceCommand.getIsUpdated()) {
                    asyncResponse.resume(ResponseFactory.response(
                            OK,
                            deviceCommand,
                            COMMAND_TO_DEVICE));
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
                    commandService.sendUnsubscribeRequest(Collections.singleton(future.get().getLeft()));
                } catch (InterruptedException | ExecutionException e) {
                    if (!asyncResponse.isDone()) {
                        asyncResponse.resume(ResponseFactory.response(INTERNAL_SERVER_ERROR));
                    }
                }
            });
        } else {
            if (!asyncResponse.isDone()) {
                asyncResponse.resume(ResponseFactory.response(OK, command, COMMAND_TO_DEVICE));
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
            List<String> names = StringUtils.isNoneEmpty(command) ? Collections.singletonList(command) : Collections.emptyList();
            
            commandService.find(Collections.singletonList(deviceId), names, timestampSt, timestampEnd, status,
                    sortField, sortOrderSt, take, skip)
                    .thenApply(commands -> ResponseFactory.response(OK, commands, Policy.COMMAND_LISTED))
                    .thenAccept(asyncResponse::resume);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get(String deviceId, String commandId, boolean returnUpdatedCommands, @Suspended final AsyncResponse asyncResponse) {
        logger.debug("Device command get requested. deviceId = {}, commandId = {}", deviceId, commandId);

        DeviceVO device = deviceService.findById(deviceId);
        if (device == null) {
            Response response = ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.DEVICE_NOT_FOUND, deviceId)));
            asyncResponse.resume(response);
            return;
        }

        commandService.findOne(Long.valueOf(commandId), device.getDeviceId(), returnUpdatedCommands)
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
                    return ResponseFactory.response(OK, command.get(), COMMAND_TO_DEVICE);
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
            commandService.insert(deviceCommand, device, authUser)
                    .thenAccept(command -> {
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
                    }).exceptionally(ex -> {
                        logger.warn("Unable to insert notification.", ex);
                        throw new HiveException(Messages.INTERNAL_SERVER_ERROR, SC_INTERNAL_SERVER_ERROR);
                    });;
            
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(String deviceId, Long commandId, DeviceCommandUpdate commandUpdate, @Suspended final AsyncResponse asyncResponse) {

        logger.debug("Device command update requested. Command update {}", commandUpdate);
        DeviceVO device = deviceService.findById(deviceId);
        if (device == null) {
            logger.warn("Device command update failed. No device with id = {} found", deviceId);
            ErrorResponse errorCode = new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.DEVICE_NOT_FOUND, deviceId));
            Response response = ResponseFactory.response(NOT_FOUND, errorCode);
            asyncResponse.resume(response);
        } else {
            commandService.findOne(commandId, deviceId)
                .thenAccept(savedCommand -> {
                    if (!savedCommand.isPresent()) {
                        logger.warn("Device command update failed. No command with id = {} found for device with id = {}", commandId, deviceId);
                        Response response = ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                                String.format(Messages.COMMAND_NOT_FOUND, commandId)));
                        asyncResponse.resume(response);
                    } else {
                        logger.debug("Device command update proceed successfully deviceId = {} commandId = {}", deviceId, commandId);
                        commandService.update(savedCommand.get(), commandUpdate);
                        asyncResponse.resume(ResponseFactory.response(Response.Status.NO_CONTENT));
                    }
                }).exceptionally(ex -> {
                    logger.warn("Unable to update notification.", ex);
                    throw new HiveException(Messages.INTERNAL_SERVER_ERROR, SC_INTERNAL_SERVER_ERROR);
                });
        }
    }

}

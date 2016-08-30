package com.devicehive.websockets.handlers;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.messages.handler.ClientHandler;
import com.devicehive.messages.handler.WebSocketClientHandler;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.CommandUpdateSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.eventbus.events.CommandEvent;
import com.devicehive.model.rpc.CommandSubscribeRequest;
import com.devicehive.model.rpc.CommandSubscribeResponse;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.UserVO;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.devicehive.websockets.InsertCommand;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.devicehive.websockets.util.SubscriptionSessionMap;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.servlet.http.HttpServletResponse.*;

@Component
public class CommandHandlers extends WebsocketHandlers {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandlers.class);

    @Autowired
    private SubscriptionManager subscriptionManager;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceCommandService commandService;
    @Autowired
    private AsyncMessageSupplier asyncMessageDeliverer;
    @Autowired
    private SubscriptionSessionMap subscriptionSessionMap;
    @Autowired
    private RpcClient rpcClient;

    @Action("command/subscribe")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_COMMAND')")
    public WebSocketResponse processCommandSubscribe(@WsParam(TIMESTAMP) Date timestamp,
                                                     @WsParam(DEVICE_GUIDS) Set<String> devices,
                                                     @WsParam(NAMES) Set<String> names,
                                                     @WsParam(DEVICE_GUID) String deviceId,
                                                     WebSocketSession session) throws Exception {
        logger.debug("command/subscribe requested for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, timestamp, names, session);

        devices = prepareActualList(devices, deviceId);
        ClientHandler clientHandler = new WebSocketClientHandler(session, asyncMessageDeliverer);
        UUID subId = submitCommandSubscribe(devices, names, timestamp, clientHandler);
        logger.debug("command/subscribe done for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, timestamp, names, session);

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(SUBSCRIPTION_ID, subId, null);
        return response;
    }

    @Action("command/unsubscribe")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_COMMAND')")
    public WebSocketResponse processCommandUnsubscribe(WebSocketSession session,
                                                       @WsParam(SUBSCRIPTION_ID) UUID subId,
                                                       @WsParam(DEVICE_GUIDS) Set<String> deviceGuids) {
        logger.debug("command/unsubscribe action. Session {} ", session.getId());
        HiveWebsocketSessionState state = HiveWebsocketSessionState.get(session);
        state.getCommandSubscriptionsLock().lock();
        try {
            Set<UUID> subscriptions = new HashSet<>();
            if (subId == null) {
                if (deviceGuids == null) {
                    Set<String> subForAll = new HashSet<String>() {
                        {
                            add(Constants.NULL_SUBSTITUTE);
                        }

                        private static final long serialVersionUID = 8001668138178383978L;
                    };
                    subscriptions.addAll(state.removeOldFormatCommandSubscription(subForAll));
                } else {
                    subscriptions.addAll(state.removeOldFormatCommandSubscription(deviceGuids));
                }
            } else {
                subscriptions.add(subId);
            }
            for (UUID toUnsubscribe : subscriptions) {
                if (state.getCommandSubscriptions().contains(toUnsubscribe)) {
                    state.getCommandSubscriptions().remove(toUnsubscribe);
                    subscriptionSessionMap.remove(toUnsubscribe);
                    subscriptionManager.getCommandSubscriptionStorage().removeBySubscriptionId(toUnsubscribe);
                }
            }
        } finally {
            state.getCommandSubscriptionsLock().unlock();
            logger.debug("deliver messages process for session" + session.getId());
            asyncMessageDeliverer.deliverMessages(session);
        }
        logger.debug("command/unsubscribe completed for session {}", session.getId());
        return new WebSocketResponse();
    }

    @Action(value = "command/insert")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'CREATE_DEVICE_COMMAND')")
    public WebSocketResponse processCommandInsert(@WsParam(DEVICE_GUID) String deviceGuid,
                                                  @WsParam(COMMAND) @JsonPolicyApply(COMMAND_FROM_CLIENT)
                                                          DeviceCommandWrapper deviceCommand,
                                                  WebSocketSession session) throws IOException {
        logger.debug("command/insert action for {}, Session ", deviceGuid, session.getId());
        if (deviceGuid == null) {
            throw new HiveException(Messages.DEVICE_GUID_REQUIRED, SC_BAD_REQUEST);
        }
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DeviceVO device = deviceService.findByGuidWithPermissionsCheck(deviceGuid, principal);
        if (device == null) {
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceGuid), SC_NOT_FOUND);
        }
        if (deviceCommand == null) {
            throw new HiveException(Messages.EMPTY_COMMAND, SC_BAD_REQUEST);
        }
        final UserVO user = principal.getUser() != null ? principal.getUser() : principal.getKey().getUser();

        WebSocketResponse response = new WebSocketResponse();
        commandService.insert(deviceCommand, device, user)
                .thenApply(cmd -> {
                    commandUpdateSubscribeAction(session, cmd.getId());
                    response.addValue(COMMAND, new InsertCommand(cmd.getId(), cmd.getTimestamp(), cmd.getUserId()), COMMAND_TO_CLIENT);
                    return response;
                })
                .exceptionally(ex -> {
                    logger.warn("Unable to insert notification.", ex);
                    throw new HiveException(Messages.INTERNAL_SERVER_ERROR, SC_INTERNAL_SERVER_ERROR);
                }).join();
        return response;
    }

    @Action("command/update")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'UPDATE_DEVICE_COMMAND')")
    public WebSocketResponse processCommandUpdate(@WsParam(DEVICE_GUID) String guid,
                                                  @WsParam(COMMAND_ID) Long id,
                                                  @WsParam(COMMAND)
                                                  @JsonPolicyApply(COMMAND_UPDATE_FROM_DEVICE)
                                                          DeviceCommandWrapper commandUpdate,
                                                  WebSocketSession session) {
        logger.debug("command/update requested for session: {}. Device guid: {}. Command id: {}", session, guid, id);
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (guid == null) {
            if (principal.getDevice() != null) {
                guid = principal.getDevice().getGuid();
            }
        }
        if (guid == null) {
            logger.debug("command/update canceled for session: {}. Guid is not provided", session);
            throw new HiveException(Messages.DEVICE_GUID_REQUIRED, SC_BAD_REQUEST);
        }
        if (id == null) {
            logger.debug("command/update canceled for session: {}. Command id is not provided", session);
            throw new HiveException(Messages.COMMAND_ID_REQUIRED, SC_BAD_REQUEST);
        }
        //TODO [rafa] unused local variable?
        final UserVO user = principal.getUser() != null ? principal.getUser() :
                (principal.getKey() != null ? principal.getKey().getUser() : null);
        DeviceVO device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (commandUpdate == null || device == null) {
            throw new HiveException(String.format(Messages.COMMAND_NOT_FOUND, id), SC_NOT_FOUND);
        }
        commandService.update(id, guid, commandUpdate);

        logger.debug("command/update proceed successfully for session: {}. Device guid: {}. Command id: {}", session,
                guid, id);
        return new WebSocketResponse();
    }

    private UUID submitCommandSubscribe(final Set<String> devices,
                                        final Set<String> names,
                                        final Date timestamp,
                                        final ClientHandler clientHandler) throws InterruptedException {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (names != null && names.isEmpty()) {
            throw new HiveException(Messages.EMPTY_NAMES, SC_BAD_REQUEST);
        }

        List<DeviceVO> actualDevices;
        if (devices != null) {
            actualDevices = deviceService.findByGuidWithPermissionsCheck(devices, principal);
            if (actualDevices.size() != devices.size()) {
                throw new HiveException(String.format(Messages.DEVICES_NOT_FOUND, devices), SC_FORBIDDEN);
            }
        }

        UUID subscriptionId = UUID.randomUUID();
        Set<CommandSubscribeRequest> subscribeRequests = devices.stream()
                .map(device -> new CommandSubscribeRequest(subscriptionId.toString(), device, names, timestamp))
                .collect(Collectors.toSet());

        CountDownLatch responseLatch = new CountDownLatch(subscribeRequests.size());
        Set<DeviceCommand> commands = new HashSet<>();
        for (CommandSubscribeRequest subscribeRequest : subscribeRequests) {
            Consumer<Response> callback = response -> {
                String resAction = response.getBody().getAction();
                if (resAction.equals(com.devicehive.model.rpc.Action.COMMAND_SUBSCRIBE_RESPONSE.name())) {
                    CommandSubscribeResponse subscribeResponse = (CommandSubscribeResponse) response.getBody();
                    commands.addAll(subscribeResponse.getCommands());
                    responseLatch.countDown();
                } else if (resAction.equals(com.devicehive.model.rpc.Action.COMMAND.name())) {
                    CommandEvent event = (CommandEvent) response.getBody();
                    JsonObject json = ServerResponsesFactory.createCommandInsertMessage(event.getCommand(), subscriptionId);
                    clientHandler.sendMessage(json);
                } else {
                    logger.warn("Unknown action received from backend {}", resAction);
                }
            };

            Request request = Request.newBuilder()
                    .withBody(subscribeRequest)
                    .withPartitionKey(subscribeRequest.getDevice())
                    .withCorrelationId(UUID.randomUUID().toString())
                    .withSingleReply(false)
                    .build();
            rpcClient.call(request, callback);
        }

        responseLatch.await();
        return subscriptionId;
    }

    private Set<String> prepareActualList(Set<String> deviceIdSet, final String deviceId) {
        if (deviceId == null && deviceIdSet == null) {
            return null;
        }
        if (deviceIdSet != null && deviceId == null) {
            deviceIdSet.remove(null);
            return deviceIdSet;
        }
        if (deviceIdSet == null) {
            return new HashSet<String>() {
                {
                    add(deviceId);
                }

                private static final long serialVersionUID = -8657632518613033661L;
            };
        }
        throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, SC_BAD_REQUEST);

    }

    private void commandUpdateSubscribeAction(WebSocketSession session, Long commandId) {
        if (commandId == null) {
            throw new HiveException(String.format(Messages.COLUMN_CANNOT_BE_NULL, "commandId"), SC_BAD_REQUEST);
        }
        HiveWebsocketSessionState state = HiveWebsocketSessionState.get(session);
        state.getCommandUpdateSubscriptionsLock().lock();
        try {
            logger.debug("commandUpdate/subscribe action. Session {}", session.getId());
            UUID reqId = UUID.randomUUID();
            CommandUpdateSubscription subscription = new CommandUpdateSubscription(commandId, reqId,
                    WebsocketHandlerCreator.createCommandUpdate(session));
            subscriptionSessionMap.put(reqId, session);
            state.getCommandUpdateSubscriptions().add(reqId);
            subscriptionManager.getCommandUpdateSubscriptionStorage().insert(subscription);
        } finally {
            HiveWebsocketSessionState.get(session).getCommandUpdateSubscriptionsLock().unlock();
            logger.debug("deliver messages process for session" + session.getId());
            asyncMessageDeliverer.deliverMessages(session);
        }
    }
}

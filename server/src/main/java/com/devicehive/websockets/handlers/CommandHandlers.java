package com.devicehive.websockets.handlers;


import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.User;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.converters.JsonMessageBuilder;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WebsocketController;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.devicehive.websockets.util.WebsocketSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.websocket.Session;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

import static com.devicehive.auth.AllowedKeyAction.Action.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;


@WebsocketController
@LogExecutionTime
public class CommandHandlers implements WebsocketHandlers {

    private static final Logger logger = LoggerFactory.getLogger(CommandHandlers.class);
    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private DeviceService deviceService;
    @EJB
    private DeviceCommandService commandService;
    @EJB
    private AsyncMessageSupplier asyncMessageDeliverer;
    @EJB
    private TimestampService timestampService;

    public static String createAccessDeniedForGuidsMessage(List<String> guids,
                                                           List<Device> allowedDevices) {
        Set<String> guidsWithDeniedAccess = new HashSet<>();
        Set<String> allowedGuids = new HashSet<>(allowedDevices.size());
        for (Device device : allowedDevices) {
            allowedGuids.add(device.getGuid());
        }
        for (String deviceGuid : guids) {
            if (!allowedGuids.contains(deviceGuid)) {
                guidsWithDeniedAccess.add(deviceGuid);
            }
        }
        StringBuilder message = new StringBuilder("No access to devices with guids: {");
        message.append(StringUtils.join(guidsWithDeniedAccess.toArray(), ", "));
        message.append("}");
        return message.toString();
    }

    @Action("command/subscribe")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.DEVICE, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE_COMMAND})
    public WebSocketResponse processCommandSubscribe(@WsParam(JsonMessageBuilder.TIMESTAMP) Timestamp timestamp,
                                                     @WsParam(JsonMessageBuilder.DEVICE_GUIDS) List<String> list,
                                                     @WsParam("deviceId") String deviceId,
                                                     Session session) throws IOException {
        logger.debug("command/subscribe requested for devices: {}, {}. Timestamp: {}. Session: {}",
                list, deviceId, timestamp, session);
        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }
        Device device = ThreadLocalVariablesKeeper.getPrincipal().getDevice();
        if (device != null) {
            deviceSubscribeAction(timestamp, session, device);
        } else {
            List<String> actualList = prepareActualList(list, deviceId);
            if (actualList == null) {
                prepareForCommandsSubscribeNullCase(session, timestamp);
            } else {
                prepareForCommandsSubscribeNotNullCase(actualList, session, timestamp);
            }
        }
        logger.debug("command/subscribe proceed successfully for devices: {}, {}. Timestamp: {}. Session: {}",
                list, deviceId, timestamp, session);

        return new WebSocketResponse();
    }

    private List<String> prepareActualList(List<String> deviceIdList, String deviceId) {
        if (deviceId == null && deviceIdList == null) {
            return null;
        }
        List<String> actualList = new ArrayList<>();
        if (deviceIdList != null) {
            actualList.addAll(deviceIdList);
        }
        if (deviceId != null) {
            actualList.add(deviceId);
        }
        return actualList;
    }

    private void prepareForCommandsSubscribeNullCase(Session session, Timestamp timestamp) throws IOException {
        logger.debug("notification/subscribe action - null guid case. Session {}", session.getId());
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        User user = principal.getUser();
        if (user == null)
            user = principal.getKey().getUser();
        List<DeviceCommand> deviceCommands = commandService.getNewerThan(null, user, timestamp);
        logger.debug(
                "notification/subscribe action - null guid case. get device notification. found {}  notifications. {}",
                deviceCommands.size(), session.getId());
        commandsSubscribeAction(deviceCommands, session, null);
    }

    private void prepareForCommandsSubscribeNotNullCase(List<String> guids,
                                                        Session session,
                                                        Timestamp timestamp) throws IOException {
        logger.debug("commands/subscribe action - null guid case. Session {}", session.getId());
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        List<Device> devices = deviceService.findByGuidWithPermissionsCheck(guids, principal);
        if (devices.size() != guids.size()) {
            String message = createAccessDeniedForGuidsMessage(guids, devices);
            throw new HiveException(message, SC_NOT_FOUND);
        }
        logger.debug("Found " + devices.size() + " devices" + ". Session " + session.getId());
        User user = principal.getUser();
        if (user == null)
            user = principal.getKey().getUser();
        List<DeviceCommand> deviceCommands = commandService.getNewerThan(devices, user, timestamp);
        commandsSubscribeAction(deviceCommands, session, devices);
    }

    private void commandsSubscribeAction(List<DeviceCommand> deviceCommands, Session session,
                                         List<Device> devices) throws IOException {
        try {
            logger.debug("command/subscribe action - not null guid case. found {} devices. Session {}",
                    deviceCommands.size(), session.getId());
            WebsocketSession.getCommandsSubscriptionsLock(session).lock();
            if (devices != null) {
                List<CommandSubscription> csList = new ArrayList<>();
                for (Device device : devices) {
                    CommandSubscription cs =
                            new CommandSubscription(ThreadLocalVariablesKeeper.getPrincipal(), device.getId(),
                                    session.getId(),
                                    null,
                                    new WebsocketHandlerCreator(session,
                                            WebsocketSession.COMMANDS_SUBSCRIPTION_LOCK,
                                            asyncMessageDeliverer));
                    csList.add(cs);
                }
                subscriptionManager.getCommandSubscriptionStorage().insertAll(csList);
            } else {
                CommandSubscription forAll =
                        new CommandSubscription(ThreadLocalVariablesKeeper.getPrincipal(),
                                Constants.DEVICE_COMMAND_NULL_ID_SUBSTITUTE,
                                session.getId(),
                                null,
                                new WebsocketHandlerCreator(session, WebsocketSession.COMMANDS_SUBSCRIPTION_LOCK,
                                        asyncMessageDeliverer));
                subscriptionManager.getCommandSubscriptionStorage().insert(forAll);
            }
            if (!deviceCommands.isEmpty()) {
                for (DeviceCommand deviceCommand : deviceCommands) {
                    WebsocketSession.addMessagesToQueue(session,
                            ServerResponsesFactory.createCommandInsertMessage(deviceCommand));
                }
            }
        } finally {
            WebsocketSession.getCommandsSubscriptionsLock(session).unlock();
            logger.debug("deliver messages process for session" + session.getId());
            asyncMessageDeliverer.deliverMessages(session);
        }
    }

    private void deviceSubscribeAction(Timestamp timestamp, Session session, Device device) {
        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }
        try {
            WebsocketSession.getCommandsSubscriptionsLock(session).lock();
            logger.debug("will subscribe device for commands : " + device.getGuid());

            CommandSubscription commandSubscription = new CommandSubscription(
                    ThreadLocalVariablesKeeper.getPrincipal(),
                    device.getId(),
                    session.getId(),
                    null,
                    new WebsocketHandlerCreator(session, WebsocketSession.COMMANDS_SUBSCRIPTION_LOCK,
                            asyncMessageDeliverer));
            subscriptionManager.getCommandSubscriptionStorage().insert(commandSubscription);


            logger.debug("will get commands newer than : {}", timestamp);
            List<DeviceCommand> commandsFromDatabase =
                    commandService.getNewerThan(Arrays.asList(device), null, timestamp);
            for (DeviceCommand deviceCommand : commandsFromDatabase) {
                logger.debug("will add command to queue : {}", deviceCommand.getId());
                WebsocketSession
                        .addMessagesToQueue(session, ServerResponsesFactory.createCommandInsertMessage(deviceCommand));
            }
        } finally {
            WebsocketSession.getCommandsSubscriptionsLock(session).unlock();
        }
        logger.debug("deliver messages for session {}", session.getId());
        asyncMessageDeliverer.deliverMessages(session);
    }

    @Action("command/unsubscribe")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.DEVICE, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE_COMMAND})
    public WebSocketResponse processCommandUnsubscribe(@WsParam(JsonMessageBuilder.DEVICE_GUIDS) List<String> list,
                                                       @WsParam("deviceId") String deviceId,
                                                       Session session) {
        logger.debug("command/unsubscribe action. Session {} ", session.getId());
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        if (principal.getDevice() != null) {
            subscriptionManager.getCommandSubscriptionStorage().remove(principal.getDevice().getId(), session.getId());
        } else {
            processComandUnsubsribeForUser(list, deviceId, session);
        }

        logger.debug("command/unsubscribe completed for session {}", session.getId());
        return new WebSocketResponse();
    }

    private void processComandUnsubsribeForUser(List<String> list, String deviceId, Session session) {
        List<String> actualList = prepareActualList(list, deviceId);
        try {
            WebsocketSession.getCommandsSubscriptionsLock(session).lock();
            List<Pair<Long, String>> subs;
            if (actualList != null) {
                HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
                List<Device> devices = deviceService.findByGuidWithPermissionsCheck(actualList, principal);
                logger.debug("command/unsubscribe. found {} devices. ", devices.size());
                if (devices.size() != actualList.size()) {
                    String message = createAccessDeniedForGuidsMessage(actualList, devices);
                    throw new HiveException(message, SC_NOT_FOUND);
                }
                logger.debug("command/unsubscribe. performing unsubscribing action");
                subs = new ArrayList<>(devices.size());
                for (Device device : devices) {
                    subs.add(ImmutablePair.of(device.getId(), session.getId()));
                }
            } else {
                subs = new ArrayList<>(1);
                subs.add(ImmutablePair.of(Constants.DEVICE_COMMAND_NULL_ID_SUBSTITUTE, session.getId()));

            }
            subscriptionManager.getCommandSubscriptionStorage().removePairs(subs);
        } finally {
            WebsocketSession.getCommandsSubscriptionsLock(session).unlock();
        }
    }

    @Action(value = "command/insert")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = {CREATE_DEVICE_COMMAND})
    public WebSocketResponse processCommandInsert(@WsParam(JsonMessageBuilder.DEVICE_GUID) String deviceGuid,
                                                  @WsParam("command") @JsonPolicyApply(COMMAND_FROM_CLIENT)
                                                  DeviceCommand deviceCommand,
                                                  Session session) {
        logger.debug("command/insert action for {}, Session ", deviceGuid, session.getId());
        if (deviceGuid == null) {
            throw new HiveException("Device ID is empty", SC_BAD_REQUEST);
        }
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        Device device = deviceService.findByGuidWithPermissionsCheck(deviceGuid, principal);
        if (device == null) {
            throw new HiveException("Device with such id not found", SC_NOT_FOUND);
        }
        if (deviceCommand == null) {
            throw new HiveException("Command is empty", SC_BAD_REQUEST);
        }
        User user = principal.getUser();
        if (user == null) {
            user = principal.getKey().getUser();
        }
        deviceCommand.setUserId(user.getId());

        commandService.submitDeviceCommand(deviceCommand, device, user, session);
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("command", deviceCommand, COMMAND_TO_CLIENT);
        return response;
    }

    @Action("command/update")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.DEVICE, HiveRoles.KEY})
    @AllowedKeyAction(action = {UPDATE_DEVICE_COMMAND})
    public WebSocketResponse processCommandUpdate(@WsParam(JsonMessageBuilder.DEVICE_GUID) String guid,
                                                  @WsParam(JsonMessageBuilder.COMMAND_ID) Long id,
                                                  @WsParam(JsonMessageBuilder.COMMAND)
                                                  @JsonPolicyApply(REST_COMMAND_UPDATE_FROM_DEVICE)
                                                  DeviceCommandUpdate commandUpdate,
                                                  Session session) {
        logger.debug("command/update requested for session: {}. Device guid: {}. Command id: {}", session, guid, id);
        if (guid == null) {
            HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
            if (principal.getDevice() != null) {
                guid = principal.getDevice().getGuid();
            }
        }
        if (guid == null || id == null) {
            logger.debug("command/update canceled for session: {}. Guid or command id is not provided", session);
            throw new HiveException("Device guid and command id are required parameters!", SC_BAD_REQUEST);
        }
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (commandUpdate == null || device == null) {
            throw new HiveException("command with id " + id + " for device with " + guid + " is not found",
                    SC_NOT_FOUND);
        }
        commandUpdate.setId(id);
        commandService.submitDeviceCommandUpdate(commandUpdate, device);

        logger.debug("command/update proceed successfully for session: {}. Device guid: {}. Command id: {}", session,
                guid, id);
        return new WebSocketResponse();
    }
}

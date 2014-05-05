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
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ParseUtil;
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
                                                     @WsParam(JsonMessageBuilder.DEVICE_GUIDS) List<String> devices,
                                                     @WsParam(JsonMessageBuilder.NAMES) List<String> names,
                                                     @WsParam(JsonMessageBuilder.DEVICE_GUID) String deviceId,
                                                     Session session) throws IOException {
        logger.debug("command/subscribe requested for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, timestamp, names, session);
        devices = prepareActualList(devices, deviceId);
        UUID subId = commandsSubscribeAction(session, devices, names, timestamp);
        logger.debug("command/subscribe done for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, timestamp, names, session);

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(JsonMessageBuilder.SUBSCRIPTION, subId, null);
        return response;
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

    private UUID commandsSubscribeAction(Session session,
                                         List<String> devices,
                                         List<String> names,
                                         Timestamp timestamp) throws IOException {
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }

        try {
            logger.debug("command/subscribe action. Session {}", session.getId());
            WebsocketSession.getCommandsSubscriptionsLock(session).lock();
            List<CommandSubscription> csList = new ArrayList<>();
            UUID reqId = UUID.randomUUID();
            if (devices != null) {
                List<Device> actualDevices = deviceService.findByGuidWithPermissionsCheck(devices, principal);
                for (Device d : actualDevices) {
                    csList.add(new CommandSubscription(principal, d.getId(),
                            reqId,
                            names,
                            new WebsocketHandlerCreator(session,
                                    WebsocketSession.COMMANDS_SUBSCRIPTION_LOCK)
                    ));
                }
            } else {
                CommandSubscription forAll =
                        new CommandSubscription(principal,
                                Constants.DEVICE_COMMAND_NULL_ID_SUBSTITUTE,
                                reqId,
                                names,
                                new WebsocketHandlerCreator(session, WebsocketSession.COMMANDS_SUBSCRIPTION_LOCK));
                csList.add(forAll);
            }
            subscriptionManager.getCommandSubscriptionStorage().insertAll(csList);
            WebsocketSession.setCommandSubscriptions(session, csList);

            List<DeviceCommand> commands = commandService.getDeviceCommandsList(devices, names, timestamp, principal);
            if (!commands.isEmpty()) {
                for (DeviceCommand deviceCommand : commands) {
                    WebsocketSession.addMessagesToQueue(session,
                            ServerResponsesFactory.createCommandInsertMessage(deviceCommand));
                }
            }
            return reqId;
        } finally {
            WebsocketSession.getCommandsSubscriptionsLock(session).unlock();
            logger.debug("deliver messages process for session" + session.getId());
            asyncMessageDeliverer.deliverMessages(session);
        }
    }

    @Action("command/unsubscribe")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.DEVICE, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE_COMMAND})
    public WebSocketResponse processCommandUnsubscribe(Session session,
                                                       @WsParam(JsonMessageBuilder.SUBSCRIPTION) UUID subId) {
        logger.debug("command/unsubscribe action. Session {} ", session.getId());
        try {
            WebsocketSession.getCommandsSubscriptionsLock(session).lock();
            List<CommandSubscription> csList = WebsocketSession.removeCommandSubscriptions(session);
            subscriptionManager.getCommandSubscriptionStorage().removeAll(csList);
        } finally {
            WebsocketSession.getCommandsSubscriptionsLock(session).unlock();
            logger.debug("deliver messages process for session" + session.getId());
            asyncMessageDeliverer.deliverMessages(session);
        }
        logger.debug("command/unsubscribe completed for session {}", session.getId());
        return new WebSocketResponse();
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

        deviceCommand.setOriginSessionId(session.getId());

        commandService.submitDeviceCommand(deviceCommand, device, user);
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

package com.devicehive.websockets.handlers;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.handler.WebSocketClientHandler;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.websockets.InsertCommand;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.UserVO;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_CLIENT;
import static javax.servlet.http.HttpServletResponse.*;

@Component
public class CommandHandlers {

    private static final Logger logger = LoggerFactory.getLogger(CommandHandlers.class);

    public static final String SUBSCSRIPTION_SET_NAME = "commandSubscriptions";

    @Autowired
    private Gson gson;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceCommandService commandService;

    private final Type setType = new TypeToken<HashSet<String>>() {
    }.getType();

    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_COMMAND')")
    public WebSocketResponse processCommandSubscribe(JsonObject request, WebSocketSession session)
            throws InterruptedException {

        final Date timestamp = gson.fromJson(request.getAsJsonObject(TIMESTAMP), Date.class);
        final String deviceId = gson.fromJson(request.getAsJsonPrimitive(DEVICE_GUID), String.class);
        final Set<String> names = gson.fromJson(request.getAsJsonArray(NAMES), setType);
        Set<String> guids = gson.fromJson(request.getAsJsonArray(DEVICE_GUIDS), setType);

        logger.debug("command/subscribe requested for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                guids, deviceId, timestamp, names, session);

        guids = prepareActualList(guids, deviceId);

        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (names != null && names.isEmpty()) {
            throw new HiveException(Messages.EMPTY_NAMES, SC_BAD_REQUEST);
        }

        List<DeviceVO> actualDevices;
        if (guids != null) {
            actualDevices = deviceService.findByGuidWithPermissionsCheck(guids, principal);
            if (actualDevices.size() != guids.size()) {
                throw new HiveException(String.format(Messages.DEVICES_NOT_FOUND, guids), SC_FORBIDDEN);
            }
        }

        String subId = commandService.submitCommandSubscribe(guids, names, timestamp,
                json -> WebSocketClientHandler.sendMessage(json, session));
        logger.debug("command/subscribe done for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                guids, deviceId, timestamp, names, session);

        ((CopyOnWriteArraySet) session
                .getAttributes()
                .get(SUBSCSRIPTION_SET_NAME))
                .add(subId);

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(SUBSCRIPTION_ID, subId, null);
        return response;
    }

    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_COMMAND')")
    public WebSocketResponse processCommandUnsubscribe(JsonObject request, WebSocketSession session) {
        final String subscriptionId = request.get(SUBSCRIPTION_ID).getAsString();
        final Set<String> guids = gson.fromJson(request.getAsJsonObject(DEVICE_GUIDS), setType);

        logger.debug("command/unsubscribe action. Session {} ", session.getId());
        if (subscriptionId == null && guids == null) {
            Set<String> subForAll = new HashSet<String>() {
                {
                    add(Constants.NULL_SUBSTITUTE);
                }

                private static final long serialVersionUID = 8001668138178383978L;
            };
            commandService.submitCommandUnsubscribe(null, subForAll);
        } else if (subscriptionId != null) {
            commandService.submitCommandUnsubscribe(subscriptionId, guids);
        } else {
            commandService.submitCommandUnsubscribe(null, guids);
        }

        ((CopyOnWriteArraySet) session
                .getAttributes()
                .get(SUBSCSRIPTION_SET_NAME))
                .remove(subscriptionId);

        return new WebSocketResponse();
    }

    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'CREATE_DEVICE_COMMAND')")
    public WebSocketResponse processCommandInsert(JsonObject request, WebSocketSession session) {
        final String deviceGuid = request.get(DEVICE_GUID).getAsString();
        final DeviceCommandWrapper deviceCommand = gson
                .fromJson(request.getAsJsonObject(COMMAND), DeviceCommandWrapper.class);

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
                    commandUpdateSubscribeAction(cmd.getId(), device.getGuid());
                    response.addValue(COMMAND, new InsertCommand(cmd.getId(), cmd.getTimestamp(), cmd.getUserId()), COMMAND_TO_CLIENT);
                    return response;
                })
                .exceptionally(ex -> {
                    logger.warn("Unable to insert notification.", ex);
                    throw new HiveException(Messages.INTERNAL_SERVER_ERROR, SC_INTERNAL_SERVER_ERROR);
                }).join();
        return response;
    }

    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'UPDATE_DEVICE_COMMAND')")
    public WebSocketResponse processCommandUpdate(JsonObject request, WebSocketSession session) {
        String guid = request.get(DEVICE_GUID).getAsString();
        final Long id = Long.valueOf(request.get(COMMAND_ID).getAsString()); // TODO: nullable long?
        final DeviceCommandWrapper commandUpdate = gson
                .fromJson(request.getAsJsonObject(COMMAND), DeviceCommandWrapper.class);

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
//        final UserVO user = principal.getUser() != null ? principal.getUser() :
//                (principal.getKey() != null ? principal.getKey().getUser() : null);
        DeviceVO device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, id), SC_NOT_FOUND);
        }

        Optional<DeviceCommand> savedCommand = commandService.find(id, guid).join();
        if (!savedCommand.isPresent()) {
            throw new HiveException(String.format(Messages.COMMAND_NOT_FOUND, id), SC_NOT_FOUND);
        }
        commandService.update(savedCommand.get(), commandUpdate);

        logger.debug("command/update proceed successfully for session: {}. Device guid: {}. Command id: {}", session,
                guid, id);
        return new WebSocketResponse();
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

    private void commandUpdateSubscribeAction(Long commandId, String guid) {
        if (commandId == null) {
            throw new HiveException(String.format(Messages.COLUMN_CANNOT_BE_NULL, "commandId"), SC_BAD_REQUEST);
        }
        commandService.submitSubscribeOnUpdate(commandId, guid); // TODO: handle response
    }
}

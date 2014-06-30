package com.devicehive.client.impl;


import com.devicehive.client.CommandsController;
import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.impl.context.RestAgent;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.common.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_FROM_CLIENT;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_CLIENT;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_DEVICE;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.REST_COMMAND_UPDATE_FROM_DEVICE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

class CommandsControllerRestImpl implements CommandsController {

    private static Logger logger = LoggerFactory.getLogger(CommandsControllerRestImpl.class);
    private final RestAgent restAgent;

    CommandsControllerRestImpl(RestAgent restAgent) {
        this.restAgent = restAgent;
    }

    @SuppressWarnings("serial")
    @Override
    public List<DeviceCommand> queryCommands(String deviceGuid, Timestamp start, Timestamp end, String commandName,
                                             String status, String sortField, String sortOrder, Integer take,
                                             Integer skip, Integer gridInterval) throws HiveException {
        logger.debug("DeviceCommand: query requested for device id {}, start timestamp {], end timestamp {}, " +
                "commandName {}, status {}, sort field {}, sort order {}, take param {}, skip param {}, " +
                "grid interval {}", deviceGuid, start, end, commandName, status, sortField, sortOrder, take, skip,
                gridInterval);
        String path = "/device/" + deviceGuid + "/command";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("start", start);
        queryParams.put("end", end);
        queryParams.put("command", commandName);
        queryParams.put("status", status);
        queryParams.put("sortField", sortField);
        queryParams.put("sortOrder", sortOrder);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        queryParams.put("gridInterval", gridInterval);
        List<DeviceCommand> result = restAgent.getRestConnector().executeWithConnectionCheck(path, HttpMethod.GET,
                null, queryParams,
                new TypeToken<List<DeviceCommand>>() {
                }.getType(), COMMAND_LISTED);
        logger.debug("DeviceCommand: query request proceed successfully for device id {}, start timestamp {], " +
                "end timestamp {},commandName {}, status {}, sort field {}, sort order {}, take param {}, " +
                "skip param {}, grid interval {}",
                deviceGuid, start, end, commandName, status, sortField, sortOrder, take, skip, gridInterval);
        return result;
    }

    @Override
    public DeviceCommand getCommand(String guid, long id) throws HiveException {
        logger.debug("DeviceCommand: get requested for device id {] and command id {}", guid, id);
        String path = "/device/" + guid + "/command/" + id;
        DeviceCommand result = restAgent.getRestConnector()
                .executeWithConnectionCheck(path, HttpMethod.GET, null, DeviceCommand.class, COMMAND_TO_DEVICE);
        logger.debug("DeviceCommand: get request proceed successfully for device id {] and command id {}. Timestamp " +
                "{}, userId {}, command {], parameters {}, lifetime {}, flags {}, status {}, result {}", guid, id,
                result.getTimestamp(), result.getUserId(), result.getCommand(), result.getParameters(),
                result.getLifetime(), result.getFlags(), result.getStatus(), result.getResult());
        return result;
    }

    @Override
    public DeviceCommand insertCommand(String guid,
                                       DeviceCommand command,
                                       HiveMessageHandler<DeviceCommand> commandUpdatesHandler) throws HiveException {
        if (command == null) {
            throw new HiveClientException("Command cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("DeviceCommand: insert requested for device id {] and command: command {}, parameters {}, " +
                "lifetime {}, flags {}", guid, command.getCommand(), command.getParameters(), command.getLifetime(),
                command.getFlags());
        DeviceCommand toReturn;
        String path = "/device/" + guid + "/command";
        toReturn = restAgent.getRestConnector().executeWithConnectionCheck(path, HttpMethod.POST, null, null, command,
                DeviceCommand.class, COMMAND_FROM_CLIENT, COMMAND_TO_CLIENT);
        if (commandUpdatesHandler != null) {
            restAgent.addCommandUpdateSubscription(toReturn.getId(), guid, commandUpdatesHandler);
        }
        logger.debug("DeviceCommand: insert request proceed successfully for device id {] and command: command {}, " +
                "parameters {}, lifetime {}, flags {}. Result command id {}, timestamp {}, userId {}", guid,
                command.getCommand(), command.getParameters(), command.getLifetime(), command.getFlags(),
                toReturn.getId(), toReturn.getTimestamp(), toReturn.getUserId());
        return toReturn;
    }

    @Override
    public void updateCommand(String deviceId, DeviceCommand command) throws HiveException {
        if (command == null) {
            throw new HiveClientException("Command cannot be null!", BAD_REQUEST.getStatusCode());
        }
        if (command.getId() == null) {
            throw new HiveClientException("Command id cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("DeviceCommand: update requested for device id {] and command: id {},  flags {}, status {}, " +
                " result {}", deviceId, command.getId(), command.getFlags(), command.getStatus(), command.getResult());
        String path = "/device/" + deviceId + "/command/" + command.getId();
        restAgent.getRestConnector()
                .executeWithConnectionCheck(path, HttpMethod.PUT, null, command, REST_COMMAND_UPDATE_FROM_DEVICE);
        logger.debug("DeviceCommand: update request proceed successfully for device id {] and command: id {},  " +
                "flags {}, status {}, result {}", deviceId, command.getId(), command.getFlags(), command.getStatus(),
                command.getResult());
    }

    @Override
    public void subscribeForCommands(SubscriptionFilter filter, HiveMessageHandler<DeviceCommand> commandMessageHandler)
            throws HiveException {
        logger.debug("Device: command/subscribe requested for filter {},", filter);
        restAgent.addCommandsSubscription(filter, commandMessageHandler);
        logger.debug("Device: command/subscribe request proceed successfully for filter {},", filter);
    }

    @Override
    public void unsubscribeFromCommands(String subscriptionId) throws HiveException {
        logger.debug("Device: command/unsubscribe requested");
        restAgent.removeCommandsSubscription(subscriptionId);
        logger.debug("Device: command/unsubscribe request proceed successfully");
    }
}

package com.devicehive.client.impl;


import com.devicehive.client.CommandsController;
import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.*;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

class CommandsControllerRestImpl implements CommandsController {

    private static Logger logger = LoggerFactory.getLogger(CommandsControllerRestImpl.class);
    protected final HiveContext hiveContext;

    public CommandsControllerRestImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    @Override
    public List<DeviceCommand> queryCommands(String deviceGuid, Timestamp start, Timestamp end, String commandName,
                                             String status, String sortField, String sortOrder, Integer take,
                                             Integer skip, Integer gridInterval) {
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
        List<DeviceCommand> result = hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, queryParams,
                new TypeToken<List<DeviceCommand>>() {
                }.getType(), COMMAND_LISTED);
        logger.debug("DeviceCommand: query request proceed successfully for device id {}, start timestamp {], " +
                "end timestamp {},commandName {}, status {}, sort field {}, sort order {}, take param {}, " +
                "skip param {}, grid interval {}",
                deviceGuid, start, end, commandName, status, sortField, sortOrder, take, skip, gridInterval);
        return result;
    }

    @Override
    public DeviceCommand getCommand(String guid, long id) {
        logger.debug("DeviceCommand: get requested for device id {] and command id {}", guid, id);
        String path = "/device/" + guid + "/command/" + id;
        DeviceCommand result = hiveContext.getHiveRestClient()
                .execute(path, HttpMethod.GET, null, DeviceCommand.class, COMMAND_TO_DEVICE);
        logger.debug("DeviceCommand: get request proceed successfully for device id {] and command id {}. Timestamp " +
                "{}, userId {}, command {], parameters {}, lifetime {}, flags {}, status {}, result {}", guid, id,
                result.getTimestamp(), result.getUserId(), result.getCommand(), result.getParameters(),
                result.getLifetime(), result.getFlags(), result.getStatus(), result.getResult());
        return result;
    }

    @Override
    public DeviceCommand insertCommand(String guid, DeviceCommand command) {
        if (command == null) {
            throw new HiveClientException("Command cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("DeviceCommand: insert requested for device id {] and command: command {}, parameters {}, " +
                "lifetime {}, flags {}", guid, command.getCommand(), command.getParameters(), command.getLifetime(),
                command.getFlags());
        DeviceCommand toReturn;
        String path = "/device/" + guid + "/command";
        toReturn = hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, command,
                DeviceCommand.class, COMMAND_FROM_CLIENT, COMMAND_TO_CLIENT);
        hiveContext.getHiveSubscriptions().addCommandUpdateSubscription(toReturn.getId(), guid);
        logger.debug("DeviceCommand: insert request proceed successfully for device id {] and command: command {}, " +
                "parameters {}, lifetime {}, flags {}. Result command id {}, timestamp {}, userId {}", guid,
                command.getCommand(), command.getParameters(), command.getLifetime(), command.getFlags(),
                toReturn.getId(), toReturn.getTimestamp(), toReturn.getUserId());
        return toReturn;
    }

    @Override
    public void updateCommand(String deviceId, long id, DeviceCommand command) {
        if (command == null) {
            throw new HiveClientException("Command cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("DeviceCommand: update requested for device id {] and command: id {},  flags {}, status {}, " +
                " result {}", deviceId, id, command.getFlags(), command.getStatus(), command.getResult());
        String path = "/device/" + deviceId + "/command/" + id;
        hiveContext.getHiveRestClient()
                .execute(path, HttpMethod.PUT, null, command, REST_COMMAND_UPDATE_FROM_DEVICE);
        logger.debug("DeviceCommand: update request proceed successfully for device id {] and command: id {},  " +
                "flags {}, status {}, result {}", deviceId, id, command.getFlags(), command.getStatus(),
                command.getResult());
    }

    @Override
    public void subscribeForCommands(Timestamp timestamp, Set<String> names, String... deviceIds) {
        logger.debug("Device: command/subscribe requested for timestamp {}, names {}, device ids {}", timestamp,
                names, deviceIds);
        hiveContext.getHiveSubscriptions().addCommandsSubscription(null, timestamp, names, deviceIds);
        logger.debug("Device: command/subscribe request proceed successfully for timestamp {}, names {}, " +
                "device ids {}", timestamp, names, deviceIds);
    }

    @Override
    public void unsubscribeFromCommands(Set<String> names, String... deviceIds) {
        logger.debug("Device: command/unsubscribe requested for names {}, device ids {}", names, deviceIds);
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "command/unsubscribe");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            Gson gson = GsonFactory.createGson();
            request.add("deviceGuids", gson.toJsonTree(deviceIds));
            hiveContext.getHiveWebSocketClient().sendMessage(request);
            hiveContext.getHiveSubscriptions().removeWsCommandSubscription(names, deviceIds);
        } else {
            hiveContext.getHiveSubscriptions().removeCommandSubscription(names, deviceIds);
        }
        logger.debug("Device: command/unsubscribe request proceed successfully for names {}, device ids {}", names,
                deviceIds);
    }

    @Override
    public Queue<Pair<String, DeviceCommand>> getCommandQueue() {
        return hiveContext.getCommandQueue();
    }

    @Override
    public Queue<DeviceCommand> getCommandUpdatesQueue() {
        return hiveContext.getCommandUpdateQueue();
    }
}

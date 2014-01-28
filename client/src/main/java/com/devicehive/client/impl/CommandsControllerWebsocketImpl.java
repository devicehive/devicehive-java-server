package com.devicehive.client.impl;


import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

class CommandsControllerWebsocketImpl extends CommandsControllerRestImpl {

    private static Logger logger = LoggerFactory.getLogger(CommandsControllerWebsocketImpl.class);

    public CommandsControllerWebsocketImpl(HiveContext hiveContext) {
        super(hiveContext);
    }


    @Override
    public DeviceCommand insertCommand(String guid, DeviceCommand command) throws HiveException {
        if (command == null) {
            throw new HiveClientException("Command cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("DeviceCommand: insert requested for device id {] and command: command {}, parameters {}, " +
                "lifetime {}, flags {}", guid, command.getCommand(), command.getParameters(), command.getLifetime(),
                command.getFlags());

        JsonObject request = new JsonObject();
        request.addProperty("action", "command/insert");
        request.addProperty("deviceGuid", guid);
        Gson gson = GsonFactory.createGson(COMMAND_FROM_CLIENT);
        request.add("command", gson.toJsonTree(command));
        DeviceCommand toReturn = hiveContext.getHiveWebSocketClient().sendMessage(request, "command",
                DeviceCommand.class, COMMAND_TO_CLIENT);
        hiveContext.getWebsocketSubManager().addCommandUpdateSubscription(toReturn.getId(), guid);
        logger.debug("DeviceCommand: insert request proceed successfully for device id {] and command: command {}, " +
                "parameters {}, lifetime {}, flags {}. Result command id {}, timestamp {}, userId {}", guid,
                command.getCommand(), command.getParameters(), command.getLifetime(), command.getFlags(),
                toReturn.getId(), toReturn.getTimestamp(), toReturn.getUserId());
        return toReturn;
    }

    @Override
    public void updateCommand(String deviceId, long id, DeviceCommand command) throws HiveException {
        if (command == null) {
            throw new HiveClientException("Command cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("DeviceCommand: update requested for device id {] and command: id {},  flags {}, status {}, " +
                " result {}", deviceId, id, command.getFlags(), command.getStatus(), command.getResult());
        JsonObject request = new JsonObject();
        request.addProperty("action", "command/update");
        request.addProperty("deviceGuid", deviceId);
        request.addProperty("commandId", command.getId());
        Gson gson = GsonFactory.createGson(COMMAND_UPDATE_FROM_DEVICE);
        request.add("command", gson.toJsonTree(command));
        hiveContext.getHiveWebSocketClient().sendMessage(request);
        logger.debug("DeviceCommand: update request proceed successfully for device id {] and command: id {},  " +
                "flags {}, status {}, result {}", deviceId, id, command.getFlags(), command.getStatus(),
                command.getResult());
    }

    @Override
    public void subscribeForCommands(Timestamp timestamp, Set<String> names, String... deviceIds) throws HiveException {
        logger.debug("Device: command/subscribe requested for timestamp {}, names {}, device ids {}", timestamp,
                names, deviceIds);
        hiveContext.getWebsocketSubManager().addCommandsSubscription(timestamp, names, deviceIds);
        logger.debug("Device: command/subscribe request proceed successfully for timestamp {}, names {}, " +
                "device ids {}", timestamp, names, deviceIds);
    }

    @Override
    public void unsubscribeFromCommands(Set<String> names, String... deviceIds) throws HiveException {
        logger.debug("Device: command/unsubscribe requested for names {}, device ids {}", names, deviceIds);
        hiveContext.getWebsocketSubManager().removeCommandSubscription(names, deviceIds);
        logger.debug("Device: command/unsubscribe request proceed successfully for names {}, device ids {}", names,
                deviceIds);
    }

}

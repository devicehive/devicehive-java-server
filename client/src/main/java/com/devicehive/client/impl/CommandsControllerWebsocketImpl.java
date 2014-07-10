package com.devicehive.client.impl;


import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.impl.context.WebsocketAgent;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

class CommandsControllerWebsocketImpl extends CommandsControllerRestImpl {

    private static Logger logger = LoggerFactory.getLogger(CommandsControllerWebsocketImpl.class);
    private final WebsocketAgent websocketAgent;

    CommandsControllerWebsocketImpl(WebsocketAgent websocketAgent) {
        super(websocketAgent);
        this.websocketAgent = websocketAgent;
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

        JsonObject request = new JsonObject();
        request.addProperty("action", "command/insert");
        request.addProperty("deviceGuid", guid);
        Gson gson = GsonFactory.createGson(COMMAND_FROM_CLIENT);
        request.add("command", gson.toJsonTree(command));
        DeviceCommand toReturn = websocketAgent.sendMessage(request, "command",
                DeviceCommand.class, COMMAND_TO_CLIENT);
        if (commandUpdatesHandler != null) {
            websocketAgent.addCommandUpdateSubscription(toReturn.getId(), guid, commandUpdatesHandler);
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
        JsonObject request = new JsonObject();
        request.addProperty("action", "command/update");
        request.addProperty("deviceGuid", deviceId);
        request.addProperty("commandId", command.getId());
        Gson gson = GsonFactory.createGson(COMMAND_UPDATE_FROM_DEVICE);
        request.add("command", gson.toJsonTree(command));
        websocketAgent.sendMessage(request);
        logger.debug("DeviceCommand: update request proceed successfully for device id {] and command: id {},  " +
                        "flags {}, status {}, result {}", deviceId, command.getId(), command.getFlags(), command.getStatus(),
                command.getResult());
    }

    @Override
    public String subscribeForCommands(SubscriptionFilter filter, HiveMessageHandler<DeviceCommand> commandMessageHandler)
            throws HiveException {
        logger.debug("Client: notification/subscribe requested for filter {},", filter);

        return websocketAgent.subscribeForCommands(filter, commandMessageHandler);

    }

    @Override
    public void unsubscribeFromCommands(String subId) throws HiveException {
        logger.debug("Device: command/unsubscribe requested");
        websocketAgent.unsubscribeFromCommands(subId);
        logger.debug("Device: command/unsubscribe request proceed successfully");
    }

}

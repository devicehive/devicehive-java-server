package com.devicehive.client.impl;


import com.devicehive.client.MessageHandler;
import com.devicehive.client.impl.context.HiveWebsocketContext;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_FROM_CLIENT;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_CLIENT;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_UPDATE_FROM_DEVICE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

class CommandsControllerWebsocketImpl extends CommandsControllerRestImpl {

    private static Logger logger = LoggerFactory.getLogger(CommandsControllerWebsocketImpl.class);
    private final HiveWebsocketContext hiveContext;

    public CommandsControllerWebsocketImpl(HiveWebsocketContext hiveContext) {
        super(hiveContext);
        this.hiveContext = hiveContext;
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
        DeviceCommand toReturn = hiveContext.getWebsocketConnector().sendMessage(request, "command",
                DeviceCommand.class, COMMAND_TO_CLIENT);
        //hiveContext.getWebsocketSubManager().addCommandUpdateSubscription(toReturn.getId(), guid);
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
        hiveContext.getWebsocketConnector().sendMessage(request);
        logger.debug("DeviceCommand: update request proceed successfully for device id {] and command: id {},  " +
                "flags {}, status {}, result {}", deviceId, id, command.getFlags(), command.getStatus(),
                command.getResult());
    }

    @Override
    public void subscribeForCommands(SubscriptionFilter filter, MessageHandler<DeviceCommand> commandMessageHandler)
            throws HiveException {
        logger.debug("Client: notification/subscribe requested for filter {},", filter);

        hiveContext.addCommandsSubscription(filter, commandMessageHandler);

        logger.debug("Client: notification/subscribe proceed for filter {},", filter);
    }

    @Override
    public void unsubscribeFromCommands(String subId) throws HiveException {
        logger.debug("Device: command/unsubscribe requested");
        hiveContext.removeCommandsSubscription(subId);
        logger.debug("Device: command/unsubscribe request proceed successfully");
    }

}

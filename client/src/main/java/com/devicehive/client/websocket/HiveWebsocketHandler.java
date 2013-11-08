package com.devicehive.client.websocket;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.json.GsonFactory;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.client.model.exceptions.HiveServerException;
import com.devicehive.client.model.exceptions.InternalHiveClientException;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

public class HiveWebsocketHandler implements HiveClientEndpoint.MessageHandler {

    private final static String REQUEST_ID_MEMBER = "requestId";
    private final static String ACTION_MEMBER = "action";
    private final static String COMMAND_INSERT = "command/insert";
    private final static String COMMAND_UPDATE = "command/update";
    private final static String NOTIFICATION_INSERT = "notification/insert";
    private final static String COMMAND_MEMBER = "command";
    private final static String NOTIFICATION_MEMBER = "notification";
    private static Logger logger = LoggerFactory.getLogger(HiveWebsocketHandler.class);
    private final HiveContext hiveContext;
    private final Map<String, SettableFuture<JsonObject>> websocketResponsesMap;


    public HiveWebsocketHandler(HiveContext hiveContext, Map<String, SettableFuture<JsonObject>> responsesMap) {
        this.hiveContext = hiveContext;
        this.websocketResponsesMap = responsesMap;

    }

    @Override
    public void handleMessage(String message) {
        JsonElement elem = new JsonParser().parse(message);
        if (!elem.isJsonObject()) {
            throw new HiveServerException("Server sent unparseable message", INTERNAL_SERVER_ERROR.getStatusCode());
        }
        JsonObject jsonMessage = (JsonObject) elem;
        String deviceGuid = null;
        if (jsonMessage.has("deviceGuid")) {
            deviceGuid = jsonMessage.get("deviceGuid").getAsString();
        }
        if (!jsonMessage.has(REQUEST_ID_MEMBER)) {
            try {
                switch (jsonMessage.get(ACTION_MEMBER).getAsString()) {
                    case COMMAND_INSERT:
                        Gson commandInsertGson = GsonFactory.createGson(COMMAND_LISTED);
                        DeviceCommand commandInsert =
                                commandInsertGson.fromJson(jsonMessage.getAsJsonObject(COMMAND_MEMBER),
                                        DeviceCommand.class);
                        if (commandInsert != null) {
                            hiveContext.getCommandQueue().put(ImmutablePair.of(deviceGuid, commandInsert));
                            logger.debug("Device command inserted. Id: " + commandInsert.getId());
                        }

                        break;
                    case COMMAND_UPDATE:
                        Gson commandUpdateGson = GsonFactory.createGson(COMMAND_UPDATE_TO_CLIENT);
                        DeviceCommand commandUpdated = commandUpdateGson.fromJson(jsonMessage.getAsJsonObject
                                (COMMAND_MEMBER), DeviceCommand.class);
                        hiveContext.getCommandUpdateQueue().put(commandUpdated);
                        logger.debug("Device command updated. Id: " + commandUpdated.getId() + ". Status: " +
                                commandUpdated.getStatus());
                        break;
                    case NOTIFICATION_INSERT:
                        Gson notificationsGson = GsonFactory.createGson(NOTIFICATION_TO_CLIENT);
                        DeviceNotification notification = notificationsGson.fromJson(jsonMessage.getAsJsonObject
                                (NOTIFICATION_MEMBER), DeviceNotification.class);
                        hiveContext.getNotificationQueue().put(ImmutablePair.of(deviceGuid, notification));
                        logger.debug("Device notification inserted. Id: " + notification.getId());
                        break;
                    default: //unknown request
                        throw new HiveException("Request id is undefined");
                }
            } catch (InterruptedException e) {
                logger.info(e.getMessage(), e);
                throw new InternalHiveClientException(e.getMessage(), e);
            }
        } else {
            SettableFuture<JsonObject> future = websocketResponsesMap.get(jsonMessage.get(REQUEST_ID_MEMBER)
                    .getAsString());
            future.set(jsonMessage);
        }
    }
}

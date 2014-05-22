package com.devicehive.client.impl.websocket;


import com.devicehive.client.impl.context.HiveWebsocketContext;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.MessageHandler;
import java.util.concurrent.ConcurrentMap;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Class that is used to handle messages from server.
 */
public class HiveWebsocketHandler implements MessageHandler.Whole<String> {

    private final static String REQUEST_ID_MEMBER = "requestId";
    private final static String ACTION_MEMBER = "action";
    private final static String COMMAND_INSERT = "command/insert";
    private final static String COMMAND_UPDATE = "command/update";
    private final static String NOTIFICATION_INSERT = "notification/insert";
    private final static String COMMAND_MEMBER = "command";
    private final static String NOTIFICATION_MEMBER = "notification";
    private final static String DEVICE_GUID_MEMBER = "deviceGuid";
    private final static String SUBSCRIPTION_ID =  "subscriptionId";
    private final static Logger logger = LoggerFactory.getLogger(HiveWebsocketHandler.class);
    private final HiveWebsocketContext hiveContext;
    private final ConcurrentMap<String, SettableFuture<JsonObject>> websocketResponsesMap;


    /**
     * Constructor.
     *
     * @param hiveContext  hive context
     * @param responsesMap map that contains request id and response association.
     */
    public HiveWebsocketHandler(HiveWebsocketContext hiveContext,
                                ConcurrentMap<String, SettableFuture<JsonObject>> responsesMap) {
        this.hiveContext = hiveContext;
        this.websocketResponsesMap = responsesMap;
    }

    /**
     * Handle messages from server. If message is not a JSON object - HiveServerException will be thrown (server
     * sends smth unparseable and unexpected), else if request identifier is provided then response for some request
     * received, otherwise - command, notification or command update received.
     *
     * @param message message from server.
     */
    @Override
    public void onMessage(String message) {
        JsonObject jsonMessage;
        try {
            jsonMessage = new JsonParser().parse(message).getAsJsonObject();
            if (!jsonMessage.has(REQUEST_ID_MEMBER)) {
                try {
                    switch (jsonMessage.get(ACTION_MEMBER).getAsString()) {
                        case COMMAND_INSERT:
                            handleCommandInsert(jsonMessage);
                            break;
                        case COMMAND_UPDATE:
                            handleCommandUpdate(jsonMessage);
                            break;
                        case NOTIFICATION_INSERT:
                            handleNotification(jsonMessage);
                            break;
                        default: //unknown request
                            logger.error("Server sent unknown message {}", message);
                    }
                } catch (InterruptedException e) {
                    logger.info("Task cancelled: " + e.getMessage(), e);
                }
            } else {
                SettableFuture<JsonObject> future = websocketResponsesMap.get(jsonMessage.get(REQUEST_ID_MEMBER)
                        .getAsString());
                if (future != null) {
                    future.set(jsonMessage);
                }
            }
        } catch (JsonParseException | IllegalStateException ex) {
            logger.error("Server sent incorrect message {}", message);
        }
    }

    private void handleCommandInsert(JsonObject jsonMessage) throws InterruptedException {
        Gson commandInsertGson = GsonFactory.createGson(COMMAND_LISTED);
        DeviceCommand commandInsert = commandInsertGson.fromJson(jsonMessage.getAsJsonObject(COMMAND_MEMBER),
                DeviceCommand.class);
        String subId = jsonMessage.get(SUBSCRIPTION_ID).getAsString();
        hiveContext.getCommandsHandler(subId).handle(commandInsert);
        if (commandInsert.getTimestamp() != null) {
            // TODO update timestamp
        } else {
            logger.warn("Device command inserted without timestamp. Id: " + commandInsert.getId());
        }
        logger.debug("Device command inserted. Id: " + commandInsert.getId());

    }

    private void handleCommandUpdate(JsonObject jsonMessage) throws InterruptedException {
        Gson commandUpdateGson = GsonFactory.createGson(COMMAND_UPDATE_TO_CLIENT);
        DeviceCommand commandUpdated = commandUpdateGson.fromJson(jsonMessage.getAsJsonObject
                (COMMAND_MEMBER), DeviceCommand.class);
        hiveContext.getCommandUpdatesHandler().handle(commandUpdated);
        logger.debug("Device command updated. Id: " + commandUpdated.getId() + ". Status: " +
                commandUpdated.getStatus());
    }

    private void handleNotification(JsonObject jsonMessage) throws InterruptedException {
        Gson notificationsGson = GsonFactory.createGson(NOTIFICATION_TO_CLIENT);
        DeviceNotification notification = notificationsGson.fromJson(jsonMessage.getAsJsonObject
                (NOTIFICATION_MEMBER), DeviceNotification.class);
        String subId = jsonMessage.get(SUBSCRIPTION_ID).getAsString();
        hiveContext.getNotificationsHandler(subId).handle(notification);
        if (notification.getTimestamp() != null) {
            // TODO update timestamp
        } else {
            logger.warn("Device notification inserted without timestamp. Id: " + notification.getId());
        }
        logger.debug("Device notification inserted. Id: " + notification.getId());
    }
}

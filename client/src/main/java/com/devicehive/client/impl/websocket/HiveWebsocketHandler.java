package com.devicehive.client.impl.websocket;


import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.impl.rest.subs.WebsocketSubManager;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.client.model.exceptions.HiveServerException;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.MessageHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

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
    private final static Logger logger = LoggerFactory.getLogger(HiveWebsocketHandler.class);

    private final HiveContext hiveContext;
    private final ConcurrentMap<String, SettableFuture<JsonObject>> websocketResponsesMap;
    private final WebsocketSubManager websocketSubManager;


    /**
     * Constructor.
     *
     * @param hiveContext  hive context
     * @param responsesMap map that contains request id and response association.
     */
    public HiveWebsocketHandler(HiveContext hiveContext, ConcurrentMap<String, SettableFuture<JsonObject>> responsesMap) {
        this.hiveContext = hiveContext;
        this.websocketResponsesMap = responsesMap;
        websocketSubManager = new WebsocketSubManager(hiveContext);
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
        JsonObject jsonMessage = null;
        try {
            jsonMessage = new JsonParser().parse(message).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException ex) {
            logger.error("Server sent incorrect message {}", message);
        }
        String deviceGuid = null;
        if (jsonMessage.has(DEVICE_GUID_MEMBER)) {
            deviceGuid = jsonMessage.get(DEVICE_GUID_MEMBER).getAsString();
        }
        if (!jsonMessage.has(REQUEST_ID_MEMBER)) {
            try {
                switch (jsonMessage.get(ACTION_MEMBER).getAsString()) {
                    case COMMAND_INSERT:
                        handleCommandInsert(jsonMessage, deviceGuid);
                        break;
                    case COMMAND_UPDATE:
                        handleCommandUpdate(jsonMessage);
                        break;
                    case NOTIFICATION_INSERT:
                        handleNotification(jsonMessage, deviceGuid);
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
    }

    private void handleCommandInsert(JsonObject jsonMessage, String deviceGuid) throws InterruptedException {
        Gson commandInsertGson = GsonFactory.createGson(COMMAND_LISTED);
        DeviceCommand commandInsert = commandInsertGson.fromJson(jsonMessage.getAsJsonObject(COMMAND_MEMBER),
                DeviceCommand.class);
        hiveContext.getCommandQueue().put(ImmutablePair.of(deviceGuid, commandInsert));
        if (commandInsert.getTimestamp() != null) {
            hiveContext.getHiveSubscriptions().updateWsDeviceLastCommandTimestampAssociation(deviceGuid,
                    commandInsert.getTimestamp());
        } else {
            logger.warn("Device command inserted without timestamp. Id: " + commandInsert.getId());
        }
        logger.debug("Device command inserted. Id: " + commandInsert.getId());

    }

    private void handleCommandUpdate(JsonObject jsonMessage) throws InterruptedException {
        Gson commandUpdateGson = GsonFactory.createGson(COMMAND_UPDATE_TO_CLIENT);
        DeviceCommand commandUpdated = commandUpdateGson.fromJson(jsonMessage.getAsJsonObject
                (COMMAND_MEMBER), DeviceCommand.class);
        hiveContext.getCommandUpdateQueue().put(commandUpdated);
        hiveContext.getHiveSubscriptions().removeWsCommandUpdateSubscription(commandUpdated.getId());
        logger.debug("Device command updated. Id: " + commandUpdated.getId() + ". Status: " +
                commandUpdated.getStatus());
    }

    private void handleNotification(JsonObject jsonMessage, String deviceGuid) throws InterruptedException {
        Gson notificationsGson = GsonFactory.createGson(NOTIFICATION_TO_CLIENT);
        DeviceNotification notification = notificationsGson.fromJson(jsonMessage.getAsJsonObject
                (NOTIFICATION_MEMBER), DeviceNotification.class);
        hiveContext.getNotificationQueue().put(ImmutablePair.of(deviceGuid, notification));
        if (notification.getTimestamp() != null) {
            hiveContext.getHiveSubscriptions().updateWsDeviceLastNotificationTimestampAssociation(deviceGuid,
                    notification.getTimestamp());
        } else {
            logger.warn("Device notification inserted without timestamp. Id: " + notification.getId());
        }
        logger.debug("Device notification inserted. Id: " + notification.getId());
    }
}

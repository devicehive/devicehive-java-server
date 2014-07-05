package com.devicehive.client.impl.websocket;


import com.devicehive.client.impl.context.WebsocketAgent;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.MessageHandler;
import java.util.concurrent.ConcurrentMap;

import static com.devicehive.client.impl.websocket.JsonEncoder.*;

/**
 * Class that is used to handle messages from server.
 */
public class HiveWebsocketHandler implements MessageHandler.Whole<String> {

    private final static Logger logger = LoggerFactory.getLogger(HiveWebsocketHandler.class);
    private final WebsocketAgent websocketAgent;
    private final ConcurrentMap<String, SettableFuture<JsonObject>> websocketResponsesMap;


    /**
     * Constructor.
     *
     * @param websocketAgent
     * @param responsesMap   map that contains request id and response association.
     */
    public HiveWebsocketHandler(WebsocketAgent websocketAgent,
                                ConcurrentMap<String, SettableFuture<JsonObject>> responsesMap) {
        this.websocketAgent = websocketAgent;
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
                            websocketAgent.handleCommandInsert(jsonMessage);
                            break;
                        case COMMAND_UPDATE:
                            websocketAgent.handleCommandUpdate(jsonMessage);
                            break;
                        case NOTIFICATION_INSERT:
                            websocketAgent.handleNotification(jsonMessage);
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

}

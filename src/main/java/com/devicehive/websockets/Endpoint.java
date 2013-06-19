package com.devicehive.websockets;


import com.devicehive.exceptions.WebsocketException;
import com.devicehive.model.AuthLevel;
import com.devicehive.websockets.handlers.JsonMessageBuilder;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.HiveMessageHandlers;
import com.devicehive.websockets.json.GsonFactory;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

abstract class Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(Endpoint.class);

    private static final String ACTION = "action";
    private static final String REQUEST_ID = "requestId";

    protected abstract HiveMessageHandlers getHiveMessageHandler();

    protected static final long MAX_MESSAGE_SIZE = 10240;




    protected JsonObject processMessage(String message, Session session) {
        JsonObject response = null;

        JsonObject request = null;
        try {
            request = new JsonParser().parse(message).getAsJsonObject();
        } catch (JsonSyntaxException ex) {
            // Stop processing this request, response with simple error message (status and error fields)
            logger.error("[processMessage] Incorrect message syntax ", ex);
            return JsonMessageBuilder.createErrorResponseBuilder("Incorrect JSON syntax").build();
        }

        try {
            String action = request.getAsJsonPrimitive("action").getAsString();
            logger.debug("[action] Looking for action " + action);
            tryExecute(action, request, session);
        } catch (WebsocketException ex) {
            response = JsonMessageBuilder.createErrorResponseBuilder(ex.getMessage()).build();
        } catch (Exception ex) {
            logger.error("[processMessage] Error processing message ", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder("Internal server error").build();
        }
        return constructFinalResponse(request, response);
    }

    private JsonObject tryExecute(String action, JsonObject request, Session session) throws Exception {
        HiveMessageHandlers handler = getHiveMessageHandler();
        for (final Method method : handler.getClass().getMethods()) {
            if (method.isAnnotationPresent(Action.class)) {
                Action ann = method.getAnnotation(Action.class);
                boolean needsAuth = ann.needsAuth();
                if (needsAuth && checkAuth(request, session)) {
                    //TODO
                    //answer not authorized
                }
                if (ann.value() != null && ann.value().equals(action)) {
                    logger.trace("[tryExecute] Processing request: " + request);
                    return (JsonObject)method.invoke(handler, request, session);
                }
            }
        }
        throw new WebsocketException("Unknown action requested: " + action);
    }


    private JsonObject constructFinalResponse(JsonObject request, JsonObject response) {
        if (response == null) {
            logger.error("[constructFinalResponse]  response is null ");
            response = JsonMessageBuilder.createErrorResponseBuilder().build();
        }
        JsonObject finalResponse = new JsonMessageBuilder()
            .addAction(request.get(ACTION).getAsString())
            .addRequestId(request.get(REQUEST_ID).getAsString())
            .include(response)
            .build();
        return finalResponse;
    }


    protected abstract boolean checkAuth(JsonObject message, Session session);


}

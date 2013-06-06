package com.devicehive.websockets;


import com.devicehive.model.AuthLevel;
import com.devicehive.websockets.handlers.Action;
import com.devicehive.websockets.handlers.HiveMessageHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.websocket.Session;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

abstract class Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(Endpoint.class);

    protected abstract HiveMessageHandlers getHiveMessageHandler();

    protected static final long MAX_MESSAGE_SIZE = 10240;




    public JsonObject processMessage(JsonObject message, Session session) {
        logger.debug("[processMessage] session id " + session.getId());
        HiveMessageHandlers handler = getHiveMessageHandler();
        String action = message.getString("action");
        for (final Method method : handler.getClass().getMethods()) {
            if (method.isAnnotationPresent(Action.class)) {
                Action ann = method.getAnnotation(Action.class);
                logger.debug("[processMessage] " + ann);

                AuthLevel requiredLevel = ann.requredLevel() != null ? ann.requredLevel() : AuthLevel.NONE;
                /*
                 * TODO check actual level
                 */

                if (ann.value() != null && ann.value().equals(action)) {
                    try {
                        logger.debug("[processMessage] " + message + " " + action + " " + handler);
                        JsonObject jsonObject = (JsonObject)method.invoke(handler, message, session);
                        if (jsonObject != null) {
                            JsonObjectBuilder builder = Json.createObjectBuilder()
                                .add("action", action);
                            if (ann.copyRequestId()) {
                                JsonObject reqId = message.getJsonObject("requestId");
                                builder.add("requestId", reqId);
                            }
                            for (String key : jsonObject.keySet()) {
                                builder.add(key, jsonObject.get(key));
                            }
                            return builder.build();
                        }
                        return null;
                    } catch (IllegalAccessException e) {
                        logger.error("Error calling method " + handler.getClass().getName() + "." + method.getName(), e);
                    } catch (InvocationTargetException e) {
                        logger.error("Error calling method " + handler.getClass().getName() + "." + method.getName(), e);
                    }
                }
            }
        }
        return null;
    }



}

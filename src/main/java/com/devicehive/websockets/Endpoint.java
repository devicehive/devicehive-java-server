package com.devicehive.websockets;


import com.devicehive.model.AuthLevel;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.HiveMessageHandlers;
import com.devicehive.websockets.json.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

abstract class Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(Endpoint.class);

    protected abstract HiveMessageHandlers getHiveMessageHandler();

    protected static final long MAX_MESSAGE_SIZE = 10240;




    protected JsonObject processMessage(JsonObject message, Session session) {
        //logger.debug("[processMessage] session id " + session.getId());
        HiveMessageHandlers handler = getHiveMessageHandler();
        String action = message.getAsJsonPrimitive("action").getAsString();
        for (final Method method : handler.getClass().getMethods()) {
            if (method.isAnnotationPresent(Action.class)) {
                Action ann = method.getAnnotation(Action.class);
                logger.debug("[processMessage] " + ann);

                boolean needsAuth = ann.needsAuth();

                if (needsAuth && checkAuth(message, session)) {
                    //answer not authorized
                }
                if (ann.value() != null && ann.value().equals(action)) {
                    try {
                        logger.debug("[processMessage] " + message + " " + action + " " + handler);
                        JsonObject jsonObject = (JsonObject)method.invoke(handler, message, session);
                        if (jsonObject != null) {
                            JsonObject result = new JsonObject();
                            result.addProperty("action", action);
                            result.add("requestId", message.get("requestId"));
                            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                                result.add(entry.getKey(), entry.getValue());
                            }
                            return result;
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


    protected abstract boolean checkAuth(JsonObject message, Session session);


}

package com.devicehive.websockets;


import com.devicehive.exceptions.HiveException;
import com.devicehive.websockets.handlers.HiveMessageHandlers;
import com.devicehive.websockets.handlers.JsonMessageBuilder;
import com.devicehive.websockets.handlers.annotations.Action;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.OptimisticLockException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.websocket.Session;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

abstract class Endpoint {

    protected static final long MAX_MESSAGE_SIZE = 1024 * 1024;
    private static final Logger logger = LoggerFactory.getLogger(Endpoint.class);

    private ConcurrentMap<Pair<Class, String>, Method> methodsCache = new ConcurrentHashMap<>();

    protected JsonObject processMessage(HiveMessageHandlers handler, Reader reader, Session session) {
        JsonObject response;

        JsonObject request;
        try {
            request = new JsonParser().parse(reader).getAsJsonObject();
        } catch (JsonSyntaxException ex) {
            // Stop processing this request, response with simple error message (status and error fields)
            logger.error("[processMessage] Incorrect message syntax ", ex);
            return JsonMessageBuilder.createErrorResponseBuilder("Incorrect JSON syntax").build();
        }

        try {
            String action = request.getAsJsonPrimitive("action").getAsString();
            logger.debug("[action] Looking for action " + action);
            response = tryExecute(handler, action, request, session);
        } catch (HiveException ex) {
            logger.error("[processMessage] Error processing message ", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder(ex.getMessage()).build();
        } catch (OptimisticLockException ex) {
            logger.error("[processMessage] Error processing message ", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder("Error occurred. Please, retry again.").build();
        } catch (Exception ex) {
            logger.error("[processMessage] Error processing message ", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder("Internal server error").build();
        }
        return constructFinalResponse(request, response);
    }

    private JsonObject tryExecute(HiveMessageHandlers handler, String action, JsonObject request, Session session)
            throws IllegalAccessException, InvocationTargetException {
        ImmutablePair<Class, String> key = ImmutablePair.of((Class)handler.getClass(), action);
        Method executedMethod = methodsCache.get(key);
        if (executedMethod == null) {
            for (final Method method : handler.getClass().getMethods()) {
                if (method.isAnnotationPresent(Action.class)) {
                    if (method.getAnnotation(Action.class).value().equals(action)) {
                        executedMethod = method;
                        methodsCache.put(key, method);
                        break;
                    }
                }
            }
        }
        if (executedMethod == null) {
            throw new HiveException("Unknown action requested: " + action);
        }
        if (executedMethod.getAnnotation(Action.class).needsAuth()) {
            handler.ensureAuthorised(request, session);
        }
        try {
            return (JsonObject) executedMethod.invoke(handler, request, session);
        } catch (InvocationTargetException e) {
            invocationTargetExceptionResolve(e);
            throw e;
        }
    }

    private void invocationTargetExceptionResolve(InvocationTargetException e) throws InvocationTargetException {
        if (e.getTargetException() instanceof HiveException) {
            throw (HiveException) e.getTargetException();
        }
        if (e.getTargetException() instanceof OptimisticLockException) {
            throw (OptimisticLockException) e.getTargetException();
        }
        if (e.getTargetException() instanceof ConstraintViolationException) {
            ConstraintViolationException ex = (ConstraintViolationException) e.getTargetException();
            logger.debug("[processMessage] Validation error, incorrect input");
            Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();
            StringBuilder builderForResponse = new StringBuilder("Validation failed: \n");
            for (ConstraintViolation<?> constraintViolation : constraintViolations) {
                builderForResponse.append(constraintViolation.getMessage());
                builderForResponse.append("\n");
            }
            throw new HiveException(builderForResponse.toString());
        }
        if (e.getTargetException() instanceof JsonSyntaxException) {
            JsonSyntaxException ex = (JsonSyntaxException) e.getTargetException();
            throw new HiveException("Incorrect JSON syntax: " + ex.getCause().getMessage(), ex);
        }
        if (e.getTargetException() instanceof JsonParseException) {
            JsonParseException ex = (JsonParseException) e.getTargetException();
            throw new HiveException("Error occurred on parsing JSON object: " + ex.getMessage(), ex);
        }
        if (e.getTargetException() instanceof org.hibernate.exception.ConstraintViolationException){
            org.hibernate.exception.ConstraintViolationException target = (org.hibernate.exception
                    .ConstraintViolationException) e.getTargetException();
            throw new HiveException("Unable to proceed requests, cause unique constraint is broken on unique fields: " +
                     target.getMessage(), target);
        }
        throw e;
    }

    private JsonObject constructFinalResponse(JsonObject request, JsonObject response) {
        if (response == null) {
            logger.error("[constructFinalResponse]  response is null ");
            response = JsonMessageBuilder.createErrorResponseBuilder().build();
        }
        JsonObject finalResponse = new JsonMessageBuilder()
                .addAction(request.get(JsonMessageBuilder.ACTION).getAsString())
                .addRequestId(request.get(JsonMessageBuilder.REQUEST_ID))
                .include(response)
                .build();
        return finalResponse;
    }


}

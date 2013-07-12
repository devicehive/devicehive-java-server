package com.devicehive.websockets;


import com.devicehive.exceptions.HiveException;
import com.devicehive.websockets.handlers.HiveMessageHandlers;
import com.devicehive.websockets.handlers.JsonMessageBuilder;
import com.devicehive.websockets.handlers.annotations.Action;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.PersistenceException;
import javax.transaction.RollbackException;
import javax.transaction.TransactionalException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.websocket.Session;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

abstract class Endpoint {

    protected static final long MAX_MESSAGE_SIZE = 10240;
    private static final Logger logger = LoggerFactory.getLogger(Endpoint.class);

    protected JsonObject processMessage(HiveMessageHandlers handler, String message, Session session) {
        JsonObject response;

        JsonObject request;
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
            response = tryExecute(handler, action, request, session);
        } catch (HiveException ex) {
            response = JsonMessageBuilder.createErrorResponseBuilder(ex.getMessage()).build();
        } catch (JsonSyntaxException ex) {
            return JsonMessageBuilder.createErrorResponseBuilder("Incorrect JSON syntax: " + ex.getCause().getLocalizedMessage()).build();
        } catch(JsonParseException ex){
           return JsonMessageBuilder.createErrorResponseBuilder(ex.getLocalizedMessage()).build();
        }catch (ConstraintViolationException ex) {
            Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();
            StringBuilder builderForResponse = new StringBuilder("[processMessage] Validation failed: \n");
            for (ConstraintViolation<?> constraintViolation : constraintViolations) {
                builderForResponse.append(constraintViolation.getMessage());
                builderForResponse.append("\n");
            }
            response = JsonMessageBuilder.createErrorResponseBuilder(builderForResponse.toString()).build();
        } catch (Exception ex) {

            logger.error("[processMessage] Error processing message ", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder("Internal server error").build();
        }
        return constructFinalResponse(request, response);
    }

    private JsonObject tryExecute(HiveMessageHandlers handler, String action, JsonObject request, Session session)
            throws IllegalAccessException, InvocationTargetException {
        for (final Method method : handler.getClass().getMethods()) {
            if (method.isAnnotationPresent(Action.class)) {

                Action ann = method.getAnnotation(Action.class);
                if (ann.value().equals(action)) {
                    if (ann.needsAuth()) {
                        handler.ensureAuthorised(request, session);
                    }
                    try {
                        return (JsonObject) method.invoke(handler, request, session);
                    } catch (InvocationTargetException e) {
                       invocationTargetExceptionResolve(e);
                    }
                }
            }
        }
        throw new HiveException("Unknown action requested: " + action);
    }


    private void invocationTargetExceptionResolve(InvocationTargetException e) throws InvocationTargetException {
        if (e.getTargetException() instanceof HiveException) {
            throw new HiveException(e.getTargetException().getMessage(), e);
        }
        if (e.getTargetException() instanceof JsonSyntaxException) {
            throw (JsonSyntaxException) e.getTargetException();
        }
        if (e.getTargetException() instanceof JsonParseException){
            throw (JsonParseException) e.getTargetException();
        }
        if (e.getTargetException() instanceof TransactionalException) {
            TransactionalException target = (TransactionalException) e.getTargetException();
            target.getCause();
            if (target.getCause() instanceof RollbackException) {
                RollbackException rollbackException = (RollbackException) target.getCause();
                if (rollbackException.getCause() instanceof PersistenceException) {
                    PersistenceException persistenceException = (PersistenceException)
                            rollbackException.getCause();
                    if (persistenceException.getCause() instanceof ConstraintViolationException) {
                        ConstraintViolationException constraintViolationException =
                                (ConstraintViolationException) persistenceException.getCause();
                        throw new ConstraintViolationException(constraintViolationException
                                .getMessage(), constraintViolationException.getConstraintViolations());
                    }
                }
            }
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

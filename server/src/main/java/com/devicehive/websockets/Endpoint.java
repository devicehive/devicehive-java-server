package com.devicehive.websockets;


import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.websockets.handlers.ClientMessageHandlers;
import com.devicehive.websockets.handlers.DeviceMessageHandlers;
import com.devicehive.websockets.handlers.HiveMessageHandlers;
import com.devicehive.websockets.handlers.JsonMessageBuilder;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.devicehive.websockets.util.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.util.WebSocketResponse;
import com.google.gson.*;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.OptimisticLockException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.websocket.Session;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

abstract class Endpoint {

    protected static final long MAX_MESSAGE_SIZE = 1024 * 1024;
    private static final Logger logger = LoggerFactory.getLogger(Endpoint.class);
    private static Set<Class> HANDLERS_SET = new HashSet<Class>(){
        {
            add(DeviceMessageHandlers.class);
            add(ClientMessageHandlers.class);
        }

        private static final long serialVersionUID = -7417770184838061839L;
    };
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
        } catch (JsonSyntaxException ex){
            response = JsonMessageBuilder.createErrorResponseBuilder(ex.getLocalizedMessage()).build();
        } catch (JsonParseException ex){
            response = JsonMessageBuilder.createErrorResponseBuilder(ex.getLocalizedMessage()).build();
        }catch (Exception ex) {
            logger.error("[processMessage] Error processing message ", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder("Internal server error").build();
        }
        return constructFinalResponse(request, response);
    }

    private JsonObject tryExecute(HiveMessageHandlers handler, String action, JsonObject request,
                                  Session session)
            throws IllegalAccessException, InvocationTargetException {
        ImmutablePair<Class, String> key = ImmutablePair.of((Class) handler.getClass(), action);
        Method executedMethod = methodsCache.get(key);
        if (executedMethod == null) {
            Class currentClass = null;
            for (Class clazz : HANDLERS_SET) {
                if (clazz.isInstance(handler)) {
                    currentClass = clazz;
                    break;
                }
            }
            if (currentClass == null) {
                throw new IllegalAccessException("No handler available for " + handler.getClass().getCanonicalName());
            }
            for (final Method method : currentClass.getMethods()) {
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
        try {
            Type[] parameterTypes = executedMethod.getGenericParameterTypes();
            List<Type> parametersTypesList = Arrays.asList(parameterTypes);
            List<Object> realArguments = new LinkedList<>();
            List<Annotation[]> allAnnotations = Arrays.asList(executedMethod.getParameterAnnotations());
            Iterator<Annotation[]> iteratorForAnnotations = allAnnotations.iterator();
            for (Type currentType : parametersTypesList) {
                if (Session.class.equals(currentType)) {
                    realArguments.add(session);
                } else {
                    if (JsonObject.class.equals(currentType)) {
                        realArguments.add(request);
                    } else {
                        String jsonFieldName = null;
                        JsonPolicyDef.Policy jsonPolicy = null;
                        if (iteratorForAnnotations.hasNext()) {
                            List<Annotation> parameterAnnotations = Arrays.asList(iteratorForAnnotations.next());
                            for (Annotation currentParamAnnotation : parameterAnnotations) {
                                if (currentParamAnnotation instanceof WsParam) {
                                    jsonFieldName = ((WsParam) currentParamAnnotation).value();
                                }
                                if (currentParamAnnotation instanceof JsonPolicyDef) {
                                    jsonPolicy = ((JsonPolicyDef) currentParamAnnotation).value()[0];
                                }
                            }
                            if (jsonFieldName == null) {
                                throw new IllegalAccessException("No name specified for param with type : " +
                                        currentType);
                            }

                        }
                        Gson gson = jsonPolicy == null ? GsonFactory.createGson() : GsonFactory.createGson
                                (jsonPolicy);
                        realArguments.add(gson.fromJson(request.get(jsonFieldName), currentType));
                    }
                }
            }

            ThreadLocalVariablesKeeper.setRequest(request);
            ThreadLocalVariablesKeeper.setSession(session);
            WebSocketResponse webSocketResponse = ((WebSocketResponse) executedMethod.invoke(handler,
                    realArguments.toArray()));
            return webSocketResponse.getResponseAsJson();
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
        if (e.getTargetException() instanceof org.hibernate.exception.ConstraintViolationException) {
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

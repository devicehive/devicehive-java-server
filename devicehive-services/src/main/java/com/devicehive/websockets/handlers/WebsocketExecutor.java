package com.devicehive.websockets.handlers;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.converters.JsonMessageBuilder;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

@Component
public class WebsocketExecutor {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketExecutor.class);

    @Autowired
    private List<WebsocketHandlers> handlers;

    private ConcurrentMap<String, Pair<WebsocketHandlers, Method>> methodsCache = Maps.newConcurrentMap();
    private ConcurrentMap<Method, List<WebsocketParameterDescriptor>> parametersCache = Maps.newConcurrentMap();

    @PostConstruct
    public void init() {
        for (WebsocketHandlers handler : handlers) {
            ReflectionUtils.doWithMethods(handler.getClass(), m -> {
                ReflectionUtils.makeAccessible(m);
                Preconditions.checkArgument(m.getReturnType().equals(WebSocketResponse.class),
                        "Method should have %s return type", WebSocketResponse.class.getName());
                Action action = m.getAnnotation(Action.class);
                methodsCache.put(action.value(), Pair.of(handler, m));
            }, m -> m.isAnnotationPresent(Action.class));
        }
    }

    public JsonObject execute(JsonObject request, WebSocketSession session) {
        JsonObject response = null;
        try {
            ThreadLocalVariablesKeeper.setRequest(request);
            ThreadLocalVariablesKeeper.setSession(session);
            response = tryExecute(request, session);
        } catch (BadCredentialsException ex) {
            logger.error("Unauthorized access", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials").build();
        } catch (AccessDeniedException ex) {
            logger.error("Access to action is denied", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized").build();
        } catch (HiveException ex) {
            logger.error("Error executing the request", ex);
            response = JsonMessageBuilder.createError(ex).build();
        } catch (ConstraintViolationException ex) {
            logger.error("Error executing the request", ex);
            response =
                JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage())
                    .build();
        } catch (org.hibernate.exception.ConstraintViolationException ex) {
            logger.error("Error executing the request", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_CONFLICT, ex.getMessage())
                .build();
        } catch (JsonParseException ex) {
            logger.error("Error e   xecuting the request", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_BAD_REQUEST,
                                                                     Messages.INVALID_REQUEST_PARAMETERS).build();
        } catch (OptimisticLockException ex) {
            logger.error("Error executing the request", ex);
            logger.error("Data conflict", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_CONFLICT,
                                                                     Messages.CONFLICT_MESSAGE).build();
        } catch (PersistenceException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                response =
                    JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_CONFLICT, ex.getMessage())
                        .build();
            } else {
                response = JsonMessageBuilder
                    .createErrorResponseBuilder(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage())
                    .build();
            }
        } catch (Exception ex) {
            logger.error("Error executing the request", ex);
            response = JsonMessageBuilder
                .createErrorResponseBuilder(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage()).build();
        } finally {
            ThreadLocalVariablesKeeper.setRequest(null);
            ThreadLocalVariablesKeeper.setSession(null);
        }

        return new JsonMessageBuilder()
            .addAction(request.get(JsonMessageBuilder.ACTION))
            .addRequestId(request.get(JsonMessageBuilder.REQUEST_ID))
            .include(response)
            .build();
    }

    public JsonObject tryExecute(JsonObject request, WebSocketSession session) {
        Pair<WebsocketHandlers, Method> methodPair = getMethod(request);
        List<Object> args = prepareArgumentValues(methodPair.getRight(), request, session);
        WebSocketResponse response;
        try {
            response = (WebSocketResponse) methodPair.getRight().invoke(methodPair.getLeft(), args.toArray());
        } catch (InvocationTargetException ex) {
            Throwable target = ex.getTargetException();
            Throwables.propagateIfPossible(target);
            throw new HiveException(target.getMessage(), target);
        } catch (IllegalAccessException ex) {
            throw HiveException.fatal();
        }
        if (response == null) {
            logger.error("[tryExecute]  response is null ");
            return JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        } else {
            return response.getResponseAsJson();
        }
    }

    private String getAction(JsonObject request) {
        JsonElement action = request.get(JsonMessageBuilder.ACTION);
        if (action == null || !action.isJsonPrimitive()) {
            return null;
        }
        return action.getAsString();
    }


    private Pair<WebsocketHandlers, Method> getMethod(JsonObject request) {
        String action = getAction(request);
        if (action == null) {
            throw new JsonParseException("Action parameter is bad");
        }
        Pair<WebsocketHandlers, Method> methodPair = methodsCache.get(action);
        if (methodPair == null) {
            throw new HiveException(String.format(Messages.UNKNOWN_ACTION_REQUESTED_WS, action),
                                    HttpServletResponse.SC_NOT_FOUND);
        }
        return methodPair;
    }

    private List<WebsocketParameterDescriptor> getArguments(Method executedMethod) {
        List<WebsocketParameterDescriptor> descriptors = parametersCache.get(executedMethod);
        if (descriptors != null) {
            return descriptors;
        }
        Type[] parameterTypes = executedMethod.getGenericParameterTypes();
        Annotation[][] allAnnotations = executedMethod.getParameterAnnotations();
        descriptors = new ArrayList<>(parameterTypes.length);
        for (int i = 0; i < parameterTypes.length; i++) {
            Type type = parameterTypes[i];
            String name = null;
            JsonPolicyDef.Policy jsonPolicy = null;
            for (Annotation currentParamAnnotation : allAnnotations[i]) {
                if (currentParamAnnotation instanceof WsParam) {
                    name = ((WsParam) currentParamAnnotation).value();
                }
                if (currentParamAnnotation instanceof JsonPolicyApply) {
                    jsonPolicy = ((JsonPolicyApply) currentParamAnnotation).value();
                }
            }
            descriptors.add(new WebsocketParameterDescriptor(name, type, jsonPolicy));
        }
        parametersCache.put(executedMethod, descriptors);
        return descriptors;
    }

    private List<Object> prepareArgumentValues(Method executedMethod, JsonObject request, WebSocketSession session) {
        List<WebsocketParameterDescriptor> descriptors = getArguments(executedMethod);
        List<Object> values = new ArrayList<>(descriptors.size());

        for (WebsocketParameterDescriptor descriptor : descriptors) {
            Type type = descriptor.getType();
            if (WebSocketSession.class.equals(type)) {
                values.add(session);
            } else {
                String name = descriptor.getName();
                if (JsonObject.class.equals(type)) {
                    values.add(name != null ? request.getAsJsonObject(name) : request);
                } else {
                    Preconditions.checkNotNull(name);
                    Gson gson = descriptor.getPolicy() == null
                                ? GsonFactory.createGson()
                                : GsonFactory.createGson(descriptor.getPolicy());
                    values.add(gson.fromJson(request.get(name), type));
                }
            }
        }
        return values;
    }


    private static class WebsocketParameterDescriptor {

        private String name;
        private Type type;
        private JsonPolicyDef.Policy policy;

        public WebsocketParameterDescriptor(String name, Type type, JsonPolicyDef.Policy policy) {
            this.name = name;
            this.type = type;
            this.policy = policy;
        }

        private String getName() {
            return name;
        }

        private Type getType() {
            return type;
        }

        private JsonPolicyDef.Policy getPolicy() {
            return policy;
        }
    }
}

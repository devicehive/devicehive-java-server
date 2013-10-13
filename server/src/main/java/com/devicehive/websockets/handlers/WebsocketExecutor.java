package com.devicehive.websockets.handlers;

import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.converters.JsonMessageBuilder;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import javax.websocket.Session;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class WebsocketExecutor {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketExecutor.class);

    private static Set<Class<WebsocketHandlers>> HANDLERS_SET = new HashSet() {
        {
            add(CommonHandlers.class);
            add(CommandHandlers.class);
            add(NotificationHandlers.class);
            add(DeviceHandlers.class);
        }

        private static final long serialVersionUID = -7417770184838061839L;
    };

    @Inject
    private BeanManager beanManager;

    private ConcurrentMap<String, Pair<Class<WebsocketHandlers>, Method>> methodsCache = Maps.newConcurrentMap();
    private ConcurrentMap<Method, List<WebsocketParameterDescriptor>> parametersCache = Maps.newConcurrentMap();


    public JsonObject execute(JsonObject request, Session session) {

        JsonObject response = null;
        try {
            logger.debug("[execute] ");
            ThreadLocalVariablesKeeper.setRequest(request);
            ThreadLocalVariablesKeeper.setSession(session);
            response = tryExecute(request, session);
            logger.debug("[execute] building final response");
        } catch (HiveException ex) {
            response = JsonMessageBuilder.createError(ex).build();
        } catch (ConstraintViolationException ex) {
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage()).build();
        } catch (org.hibernate.exception.ConstraintViolationException ex) {
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_CONFLICT, ex.getMessage()).build();
        } catch (JsonParseException ex) {
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_BAD_REQUEST, ErrorResponse.JSON_SYNTAX_ERROR_MESSAGE).build();
        } catch (OptimisticLockException ex) {
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_CONFLICT, ErrorResponse.CONFLICT_MESSAGE).build();
        } catch (PersistenceException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_CONFLICT, ex.getMessage()).build();
            } else {
                response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage()).build();
            }
        } catch (Exception ex) {
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage()).build();
        } finally {
            ThreadLocalVariablesKeeper.setRequest(null);
            ThreadLocalVariablesKeeper.setSession(null);
        }

        logger.debug("[execute] building final response");
        return new JsonMessageBuilder()
                .addAction(request.get(JsonMessageBuilder.ACTION).getAsString())
                .addRequestId(request.get(JsonMessageBuilder.REQUEST_ID))
                .include(response)
                .build();
    }

    public JsonObject tryExecute(JsonObject request, Session session) {
        Pair<Class<WebsocketHandlers>, Method> methodPair = getMethod(request);
        List<Object> args = prepareArgumentValues(methodPair.getRight(), request, session);
        WebSocketResponse response = null;
        try {
            response = (WebSocketResponse) methodPair.getRight().invoke(getBean(methodPair.getLeft()), args.toArray());
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


    private WebsocketHandlers getBean(Class<WebsocketHandlers> clazz) {
        Bean bean = beanManager.getBeans(clazz).iterator().next();
        return (WebsocketHandlers) beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean));
    }

    private Pair<Class<WebsocketHandlers>, Method> getMethod(JsonObject request) {
        String action = request.getAsJsonPrimitive("action").getAsString();

        Pair<Class<WebsocketHandlers>, Method> methodPair = methodsCache.get(action);
        if (methodPair != null) {
            return methodPair;
        }

        for (Class<WebsocketHandlers> currentClass : HANDLERS_SET) {
            boolean found = false;
            for (final Method method : currentClass.getMethods()) {
                if (method.isAnnotationPresent(Action.class)) {
                    if (method.getAnnotation(Action.class).value().equals(action)) {
                        Preconditions.checkState(method.getReturnType().equals(WebSocketResponse.class), "Method should have %s return type", WebSocketResponse.class);
                        methodPair = ImmutablePair.of(currentClass, method);
                        found = true;
                        break;
                    }
                }
            }
            if (found) {
                break;
            }
        }
        if (methodPair == null) {
            throw new HiveException("Unknown action requested: " + action, HttpServletResponse.SC_BAD_REQUEST);
        }
        methodsCache.put(action, methodPair);
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

    private List<Object> prepareArgumentValues(Method executedMethod, JsonObject request, Session session) {
        List<WebsocketParameterDescriptor> descriptors = getArguments(executedMethod);
        List<Object> values = new ArrayList<>(descriptors.size());

        for (WebsocketParameterDescriptor descriptor : descriptors) {
            Type type = descriptor.getType();
            if (Session.class.equals(type)) {
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

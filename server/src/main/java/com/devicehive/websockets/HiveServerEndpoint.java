package com.devicehive.websockets;


import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.providers.JsonEncoder;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.utils.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.handlers.*;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebSocketResponse;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.OptimisticLockException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ServerEndpoint(value = "/websocket/{id}", encoders = {JsonEncoder.class})
@LogExecutionTime
@Singleton
public class HiveServerEndpoint {

    protected static final long MAX_MESSAGE_SIZE = 1024 * 1024;
    private static final Logger logger = LoggerFactory.getLogger(HiveServerEndpoint.class);
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

    @EJB
    private SessionMonitor sessionMonitor;

    @EJB
    private SubscriptionManager subscriptionManager;

    private ConcurrentMap<String, Pair<Class<WebsocketHandlers>, Method>> methodsCache = new ConcurrentHashMap<>();



    @OnOpen
    public void onOpen(Session session, @PathParam("id") String id) {
        logger.debug("[onOpen] session id {} ", session.getId());
        WebsocketSession.createCommandUpdatesSubscriptionsLock(session);
        WebsocketSession.createNotificationSubscriptionsLock(session);
        WebsocketSession.createCommandsSubscriptionsLock(session);
        WebsocketSession.createQueueLock(session);
        sessionMonitor.registerSession(session);
    }

    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public JsonObject onMessage(Reader reader, Session session) throws InvocationTargetException, IllegalAccessException {
        logger.debug("[onMessage] session id {} ", session.getId());
        return processMessage( reader, session);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.debug("[onClose] session id {}, close reason is {} ", session.getId(), closeReason);
        subscriptionManager.getCommandUpdateSubscriptionStorage().removeBySession(session.getId());
        subscriptionManager.getCommandSubscriptionStorage().removeBySession(session.getId());
        subscriptionManager.getNotificationSubscriptionStorage().removeBySession(session.getId());
    }

    @OnError
    public void onError(Throwable exception, Session session) {
        logger.error("[onError] ", exception);
    }




    protected JsonObject processMessage(Reader reader, Session session) {
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
            response = tryExecute(action, request, session);
        } catch (HiveException ex) {
            logger.error("[processMessage] Error processing message ", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder(ex.getMessage()).build();
        } catch (OptimisticLockException ex) {
            logger.error("[processMessage] Error processing message ", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder("Error occurred. Please, retry again.").build();
        } catch (JsonSyntaxException ex) {
            response = JsonMessageBuilder.createErrorResponseBuilder(ex.getLocalizedMessage()).build();
        } catch (JsonParseException ex) {
            response = JsonMessageBuilder.createErrorResponseBuilder(ex.getLocalizedMessage()).build();
        } catch (Exception ex) {
            logger.error("[processMessage] Error processing message ", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder("Internal server error").build();
        }
        return constructFinalResponse(request, response);
    }

    private WebsocketHandlers getBean(Class<WebsocketHandlers> clazz) {
        Bean bean = beanManager.getBeans(clazz).iterator().next();
        return (WebsocketHandlers) beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean));
    }

    private JsonObject tryExecute(String action, JsonObject request,
                                  Session session)
            throws IllegalAccessException, InvocationTargetException {


        Pair<Class<WebsocketHandlers>, Method> methodPair = methodsCache.get(action);
        if (methodPair == null) {
            for (Class<WebsocketHandlers> currentClass : HANDLERS_SET) {
                boolean found = false;
                for (final Method method : currentClass.getMethods()) {
                    if (method.isAnnotationPresent(Action.class)) {
                        if (method.getAnnotation(Action.class).value().equals(action)) {
                            methodPair = ImmutablePair.of(currentClass, method);
                            methodsCache.put(action, methodPair);
                            found = true;
                            break;
                        }
                    }
                }
                if (found) {
                    break;
                }
            }
        }
        if (methodPair == null) {
            throw new HiveException("Unknown action requested: " + action, HttpServletResponse.SC_BAD_REQUEST);
        }
        try {
            Method executedMethod = methodPair.getRight();
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
                                if (currentParamAnnotation instanceof JsonPolicyApply) {
                                    jsonPolicy = ((JsonPolicyApply) currentParamAnnotation).value();
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
            WebsocketHandlers handlers = getBean(methodPair.getLeft());
            WebSocketResponse webSocketResponse = ((WebSocketResponse) executedMethod.invoke(handlers,
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
            throw new HiveException(builderForResponse.toString(), HttpServletResponse.SC_BAD_REQUEST);
        }
        if (e.getTargetException() instanceof JsonSyntaxException) {
            JsonSyntaxException ex = (JsonSyntaxException) e.getTargetException();
            throw new HiveException("Incorrect JSON syntax: " + ex.getCause().getMessage(), ex, HttpServletResponse.SC_BAD_REQUEST);
        }
        if (e.getTargetException() instanceof JsonParseException) {
            JsonParseException ex = (JsonParseException) e.getTargetException();
            throw new HiveException("Error occurred on parsing JSON object: " + ex.getMessage(), ex, HttpServletResponse.SC_BAD_REQUEST);
        }
        if (e.getTargetException() instanceof org.hibernate.exception.ConstraintViolationException) {
            org.hibernate.exception.ConstraintViolationException target = (org.hibernate.exception
                    .ConstraintViolationException) e.getTargetException();
            throw new HiveException("Unable to proceed requests, cause unique constraint is broken on unique fields: " +
                    target.getMessage(), target, HttpServletResponse.SC_CONFLICT);
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

package com.devicehive.websockets;


import com.devicehive.exceptions.HiveException;
import com.devicehive.websockets.converters.JsonEncoder;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.websockets.handlers.*;
import com.devicehive.websockets.converters.JsonMessageBuilder;
import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.common.collect.Sets;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.OptimisticLockException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@ServerEndpoint(value = "/websocket/{endpoint}", encoders = {JsonEncoder.class})
@LogExecutionTime
@Singleton
public class HiveServerEndpoint {

    protected static final long MAX_MESSAGE_SIZE = 1024 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(HiveServerEndpoint.class);

    private static final Set<String> allowedEndpoints = Sets.newHashSet("client", "device");


    @EJB
    private SessionMonitor sessionMonitor;

    @EJB
    private SubscriptionManager subscriptionManager;

    @Inject
    private WebsocketExecutor executor;



    @OnOpen
    public void onOpen(Session session, @PathParam("endpoint") String endpoint) {
        logger.debug("[onOpen] session id {} ", session.getId());
        WebsocketSession.createCommandUpdatesSubscriptionsLock(session);
        WebsocketSession.createNotificationSubscriptionsLock(session);
        WebsocketSession.createCommandsSubscriptionsLock(session);
        WebsocketSession.createQueueLock(session);
        sessionMonitor.registerSession(session);
    }

    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public JsonObject onMessage(Reader reader, Session session)  {
        try {
            logger.debug("[onMessage] session id {} ", session.getId());
            JsonObject request = new JsonParser().parse(reader).getAsJsonObject();
            logger.debug("[onMessage] request is parsed correctly");
            return executor.execute(request,session);
        } catch (JsonParseException ex) {
            logger.error("[onMessage] Incorrect message syntax ", ex);
            return JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_BAD_REQUEST, "Incorrect JSON syntax")
                    .build();
        } catch (Exception ex) {
            return JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error")
                    .build();
        }
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



}

package com.devicehive.websockets;

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
import com.devicehive.exceptions.*;
import com.devicehive.json.GsonFactory;
import com.devicehive.messages.handler.WebSocketClientHandler;
import com.devicehive.model.SubscriptionInfo;
import com.devicehive.resource.exceptions.ExpiredTokenException;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.websockets.converters.JsonMessageBuilder;
import com.devicehive.websockets.handlers.CommandHandlers;
import com.devicehive.websockets.handlers.NotificationHandlers;
import com.devicehive.websockets.util.SessionMonitor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Component
public class DeviceHiveWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeviceHiveWebSocketHandler.class);

    private SessionMonitor sessionMonitor;
    private WebSocketRequestProcessor requestProcessor;
    private DeviceCommandService commandService;
    private DeviceNotificationService notificationService;
    private WebSocketClientHandler webSocketClientHandler;

    private int sendTimeLimit = 10 * 1000;
    private int sendBufferSizeLimit = 512 * 1024;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.debug("Opening session id {} ", session.getId());

        session = new ConcurrentWebSocketSessionDecorator(session, sendTimeLimit, sendBufferSizeLimit);
        HiveWebsocketSessionState state = new HiveWebsocketSessionState();
        session.getAttributes().put(HiveWebsocketSessionState.KEY, state);

        session.getAttributes().put(CommandHandlers.SUBSCRIPTION_SET_NAME, new CopyOnWriteArraySet<SubscriptionInfo>());
        session.getAttributes().put(NotificationHandlers.SUBSCRIPTION_SET_NAME, new CopyOnWriteArraySet<SubscriptionInfo>());
        session.getAttributes().put(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION, session.getPrincipal());

        sessionMonitor.registerSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException, InterruptedException {
        logger.debug("Session id {} ", session.getId());
        session = sessionMonitor.getSession(session.getId());
        JsonObject request = null;
        JsonObject response = null;
        try {
            request = new JsonParser().parse(message.getPayload()).getAsJsonObject();
            requestProcessor.process(request, session);
        } catch (JsonSyntaxException | IllegalStateException ex) {
            String errorMessage = "Malformed Json received.";
            logger.error("Error executing the request: {}", errorMessage);
            response = webSocketClientHandler.buildErrorResponse(HttpServletResponse.SC_BAD_REQUEST, errorMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BadCredentialsException ex) {
            logger.error("Unauthorized access: {}", ex.getMessage());
            response = webSocketClientHandler.buildErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
        } catch (ExpiredTokenException ex) {
            logger.info("Access token expired: {}", ex.getMessage());
            response = webSocketClientHandler.buildErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
        } catch (AccessDeniedException | AuthenticationCredentialsNotFoundException ex) {
            logger.error("Access to action is denied", ex.getMessage());
            response = webSocketClientHandler.buildErrorResponse(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
        } catch (InvalidPrincipalException ex) {
            logger.error("Unauthorized access", ex.getMessage());
            response = webSocketClientHandler.buildErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
        } catch (HiveException ex) {
            logger.error("Error executing the request: {}", ex.getMessage());
            response = webSocketClientHandler.buildErrorResponse(ex.getCode(), ex.getMessage());
        } catch (IllegalParametersException ex) {
            logger.error("Error executing the request: {}", ex.getMessage());
            response = webSocketClientHandler.buildErrorResponse(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        } catch (ActionNotAllowedException ex) {
            logger.error("Error executing the request: {}", ex.getMessage());
            response = webSocketClientHandler.buildErrorResponse(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
        } catch (ConstraintViolationException ex) {
            Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();
            StringBuilder errors = new StringBuilder();
            constraintViolations.forEach(exc -> errors.append(exc.getMessage()));
            logger.error("Error executing the request: {}", errors.toString());
            response = webSocketClientHandler.buildErrorResponse(HttpServletResponse.SC_BAD_REQUEST, errors.toString());
        } catch (org.hibernate.exception.ConstraintViolationException ex) {
            logger.error("Error executing the request: {}", ex.getMessage());
            response = webSocketClientHandler.buildErrorResponse(HttpServletResponse.SC_CONFLICT, ex.getMessage());
        } catch (OptimisticLockException ex) {
            logger.error("Error executing the request. Data conflict: {}", ex.getMessage());
            response = webSocketClientHandler.buildErrorResponse(HttpServletResponse.SC_CONFLICT, Messages.CONFLICT_MESSAGE);
        } catch (PersistenceException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                response = webSocketClientHandler.buildErrorResponse(HttpServletResponse.SC_CONFLICT, ex.getMessage());
            } else {
                response = webSocketClientHandler.buildErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            }
        } catch (NoSuchElementException ex) {
            logger.error("Error executing the request: {}", ex.getMessage());
            response = webSocketClientHandler.buildErrorResponse(HttpServletResponse.SC_NOT_FOUND, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Error executing the request: {}", ex.getMessage());
            response = webSocketClientHandler.buildErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }

        if (request == null && response != null) {
            webSocketClientHandler.sendMessage(response, session);
        }

        if (request != null && response != null) {
            webSocketClientHandler.sendMessage(request, response, session);
        }

    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        logger.debug("Pong received for session {}", session.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        CopyOnWriteArraySet<SubscriptionInfo> commandSubscriptions = (CopyOnWriteArraySet)
                session.getAttributes().get(CommandHandlers.SUBSCRIPTION_SET_NAME);
        commandService.sendUnsubscribeRequest(commandSubscriptions.stream()
                .map(SubscriptionInfo::getSubscriptionId).collect(Collectors.toSet()));

        CopyOnWriteArraySet<SubscriptionInfo> notificationSubscriptions = (CopyOnWriteArraySet)
                session.getAttributes().get(NotificationHandlers.SUBSCRIPTION_SET_NAME);
        notificationService.unsubscribe(notificationSubscriptions.stream()
                .map(SubscriptionInfo::getSubscriptionId).collect(Collectors.toSet()));

        sessionMonitor.removeSession(session.getId());

        if(session.isOpen()) {
            session.close();
        }
        logger.info("Websocket Connection Closed: session id {}, close status is {} ", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("Error in session {}: {}", session.getId(), exception.getMessage());
        if (exception.getMessage().contains("Connection reset by peer")) {
            afterConnectionClosed(session, CloseStatus.SESSION_NOT_RELIABLE);
            return;
        }

        JsonMessageBuilder builder;
        session = sessionMonitor.getSession(session.getId());

        if (exception instanceof JsonParseException) {
            builder = JsonMessageBuilder
                    .createErrorResponseBuilder(HttpServletResponse.SC_BAD_REQUEST, "Incorrect JSON syntax");
        } else {
            builder = JsonMessageBuilder
                    .createErrorResponseBuilder(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
        try {
            session.sendMessage(new TextMessage(GsonFactory.createGson().toJson(builder.build())));
        } catch (ClosedChannelException closedChannelException) {
            logger.error("WebSocket error: Channel is closed");
        }
    }

    @Autowired
    public void setSessionMonitor(SessionMonitor sessionMonitor) {
        this.sessionMonitor = sessionMonitor;
    }

    @Autowired
    public void setRequestProcessor(WebSocketRequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;
    }

    @Autowired
    public void setCommandService(DeviceCommandService commandService) {
        this.commandService = commandService;
    }

    @Autowired
    public void setNotificationService(DeviceNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Autowired
    public void setWebSocketClientHandler(WebSocketClientHandler webSocketClientHandler) {
        this.webSocketClientHandler = webSocketClientHandler;
    }
}

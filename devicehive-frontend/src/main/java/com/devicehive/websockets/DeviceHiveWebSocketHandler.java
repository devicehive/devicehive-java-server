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

import com.devicehive.json.GsonFactory;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.websockets.converters.JsonMessageBuilder;
import com.devicehive.websockets.handlers.CommandHandlers;
import com.devicehive.websockets.handlers.NotificationHandlers;
import com.devicehive.websockets.util.SessionMonitor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.CopyOnWriteArraySet;

public class DeviceHiveWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(DeviceHiveWebSocketHandler.class);

    @Autowired
    private SessionMonitor sessionMonitor;

    @Autowired
    private WebSocketResponseBuilder webSocketResponseBuilder;

    @Autowired
    private DeviceCommandService commandService;

    @Autowired
    private DeviceNotificationService notificationService;

    private int sendTimeLimit = 10 * 1000;
    private int sendBufferSizeLimit = 512 * 1024;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.debug("Opening session id {} ", session.getId());

        session = new ConcurrentWebSocketSessionDecorator(session, sendTimeLimit, sendBufferSizeLimit);
        HiveWebsocketSessionState state = new HiveWebsocketSessionState();
        session.getAttributes().put(HiveWebsocketSessionState.KEY, state);

        session.getAttributes().put(CommandHandlers.SUBSCSRIPTION_SET_NAME, new CopyOnWriteArraySet<String>());
        session.getAttributes().put(NotificationHandlers.SUBSCSRIPTION_SET_NAME, new CopyOnWriteArraySet<String>());
        session.getAttributes().put(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION, session.getPrincipal());

        sessionMonitor.registerSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.debug("Session id {} ", session.getId());
        session = sessionMonitor.getSession(session.getId());
        JsonObject request = new JsonParser().parse(message.getPayload()).getAsJsonObject();
        webSocketResponseBuilder.buildResponse(request, session);
//        JsonObject response = webSocketResponseBuilder.buildResponse(request, session);
//        session.sendMessage(new TextMessage(response.toString()));
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        logger.debug("Pong received for session {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        CopyOnWriteArraySet<String> commandSubscriptions = (CopyOnWriteArraySet)
                session.getAttributes().get(CommandHandlers.SUBSCSRIPTION_SET_NAME);
        for (String s : commandSubscriptions) {
            commandService.sendUnsubscribeRequest(s, null);
        }

        CopyOnWriteArraySet<String> notificationSubscriptions = (CopyOnWriteArraySet)
                session.getAttributes().get(NotificationHandlers.SUBSCSRIPTION_SET_NAME);
        for (String s : notificationSubscriptions) {
            notificationService.unsubscribe(s, null);
        }

        sessionMonitor.removeSession(session.getId());

        if(session.isOpen()) {
            session.close();
        }
        logger.warn("CONNECTION CLOSED: session id {}, close status is {} ", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("Error in session {}: {}", session.getId(), exception);
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
        session.sendMessage(
                new TextMessage(GsonFactory.createGson().toJson(builder.build())));
    }
}

package com.devicehive.websockets;


import com.devicehive.json.GsonFactory;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.websockets.converters.JsonMessageBuilder;
import com.devicehive.websockets.handlers.WebsocketExecutor;
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
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.servlet.http.HttpServletResponse;
import java.util.UUID;


abstract class AbstractWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(AbstractWebSocketHandler.class);

    @Autowired
    private SessionMonitor sessionMonitor;
    @Autowired
    private SubscriptionManager subscriptionManager;
    @Autowired
    private WebsocketExecutor executor;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.debug("Opening session id {} ", session.getId());
        HiveWebsocketSessionState state = new HiveWebsocketSessionState();
        session.getAttributes().put(HiveWebsocketSessionState.KEY, state);
        sessionMonitor.registerSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonObject request;
        try {
            logger.debug("Session id {} ", session.getId());
            request = new JsonParser().parse(message.getPayload()).getAsJsonObject();
            logger.debug("Request is parsed correctly");
        } catch (IllegalStateException ex) {
            throw new JsonParseException(ex);
        }
        JsonObject response = executor.execute(request, session);
        session.sendMessage(new TextMessage(GsonFactory.createGson().toJson(response)));
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        logger.debug("Pong received for session {}", session.getId());
        sessionMonitor.updateDeviceSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.debug("Closing session id {}, close status is {} ", session.getId(), status);
        HiveWebsocketSessionState state = HiveWebsocketSessionState.get(session);
        for (UUID subId : state.getCommandSubscriptions()) {
            subscriptionManager.getCommandSubscriptionStorage().removeBySubscriptionId(subId);
        }
        for (UUID subId : state.getNotificationSubscriptions()) {
            subscriptionManager.getNotificationSubscriptionStorage().removeBySubscriptionId(subId);
        }
        logger.debug("Session {} is closed", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("Error in session " + session.getId(), exception);
        JsonMessageBuilder builder;

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

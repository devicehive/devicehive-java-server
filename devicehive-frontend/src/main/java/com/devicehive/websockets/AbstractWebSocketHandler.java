package com.devicehive.websockets;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.configuration.Constants;
import com.devicehive.json.GsonFactory;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.websockets.converters.JsonMessageBuilder;
import com.devicehive.websockets.util.SessionMonitor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

abstract public class AbstractWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(AbstractWebSocketHandler.class);

    @Autowired
    private SessionMonitor sessionMonitor;
    @Autowired
    private SubscriptionManager subscriptionManager;
    @Autowired
    private WebSocketExecutor executor;
    @Autowired
    @Qualifier(DeviceHiveApplication.MESSAGE_EXECUTOR)
    private ExecutorService executorService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.debug("Opening session id {} ", session.getId());

        session.setBinaryMessageSizeLimit(Constants.WEBSOCKET_MAX_BUFFER_SIZE);
        session.setTextMessageSizeLimit(Constants.WEBSOCKET_MAX_BUFFER_SIZE);

        HiveWebSocketSessionState state = new HiveWebSocketSessionState();
        session.getAttributes().put(HiveWebSocketSessionState.KEY, state);
        sessionMonitor.registerSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.info("Session id {} ", session.getId());
        JsonObject request = new JsonParser().parse(message.getPayload()).getAsJsonObject();
        CompletableFuture.supplyAsync(() -> executor.execute(request, session), executorService)
                .handleAsync((ok, ex) -> {
                    String response;
                    if (ok != null) {
                        response = ok.toString();
                    } else {
                        logger.error("Unexpected exception occured during handling websocket message: {}", ex);
                        response = JsonMessageBuilder
                                    .createErrorResponseBuilder(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage())
                                    .build().toString();
                    }

                    try {
                        session.sendMessage(new TextMessage(response));
                    } catch (IOException e) {
                        logger.error("Exception handled during sending websocket message: {}", e);
                        throw new RuntimeException(e);
                    }
                    return null;
                }, executorService);
        logger.debug("Request is parsed correctly");
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        logger.debug("Pong received for session {}", session.getId());
        sessionMonitor.updateDeviceSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.debug("Closing session id {}, close status is {} ", session.getId(), status);
        HiveWebSocketSessionState state = HiveWebSocketSessionState.get(session);
        for (String subId : state.getCommandSubscriptions()) {
            subscriptionManager.getCommandSubscriptionStorage().removeBySubscriptionId(subId);
        }
        for (String subId : state.getNotificationSubscriptions()) {
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

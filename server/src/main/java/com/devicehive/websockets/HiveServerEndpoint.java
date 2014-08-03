package com.devicehive.websockets;


import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.context.WebsocketContextExtension;
import com.devicehive.websockets.converters.JsonEncoder;
import com.devicehive.websockets.converters.JsonMessageBuilder;
import com.devicehive.websockets.handlers.WebsocketExecutor;
import com.devicehive.websockets.util.SessionMonitor;
import com.google.common.base.Throwables;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.*;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.io.Reader;
import java.util.UUID;

@ServerEndpoint(value = "/websocket/{endpoint}", encoders = {JsonEncoder.class})
@LogExecutionTime
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class HiveServerEndpoint {

    protected static final long MAX_MESSAGE_SIZE = 1024 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(HiveServerEndpoint.class);

    @Inject
    private SessionMonitor sessionMonitor;

    @Inject
    private SubscriptionManager subscriptionManager;

    @Inject
    private WebsocketExecutor executor;

    @Inject
    private HiveSecurityContext hiveSecurityContext;


    @OnOpen
    public void onOpen(Session session, @PathParam("endpoint") String endpoint) {
        logger.info("Opening session id {} ", session.getId());
        HiveWebsocketSessionState state = new HiveWebsocketSessionState();
        session.getUserProperties().put(HiveWebsocketSessionState.KEY, state);
        state.setOrigin(hiveSecurityContext.getOrigin());
        state.setHivePrincipal(hiveSecurityContext.getHivePrincipal());
        state.setClientInetAddress(hiveSecurityContext.getClientInetAddress());
        sessionMonitor.registerSession(session);
    }

    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public JsonObject onMessage(Reader reader, Session session) {
        JsonObject request = null;
        try {
            logger.debug("Session id {} ", session.getId());
            request = new JsonParser().parse(reader).getAsJsonObject();
            logger.debug("Request is parsed correctly");
        } catch (IllegalStateException ex) {
            throw new JsonParseException(ex);
        }

        return executor.execute(request,session);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info("Closing session id {}, close reason is {} ", session.getId(), closeReason);
        HiveWebsocketSessionState state = HiveWebsocketSessionState.get(session);
        for (UUID subId : state.getCommandSubscriptions()) {
            subscriptionManager.getCommandSubscriptionStorage().removeBySubscriptionId(subId);
        }
        for (UUID subId : state.getNotificationSubscriptions()) {
            subscriptionManager.getNotificationSubscriptionStorage().removeBySubscriptionId(subId);
        }
    }

    @OnError
    public void onError(Throwable exception, Session session) {
        logger.error("Error in session " + session.getId(), exception);

        JsonMessageBuilder builder = null;

        if (exception instanceof JsonParseException) {
            builder = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_BAD_REQUEST, "Incorrect JSON syntax");
        } else {
            builder = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
        session.getAsyncRemote().sendText(GsonFactory.createGson().toJson(builder.build()));

    }


}

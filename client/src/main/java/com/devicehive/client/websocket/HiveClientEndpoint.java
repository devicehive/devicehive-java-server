package com.devicehive.client.websocket;


import com.devicehive.client.model.exceptions.InternalHiveClientException;
import com.devicehive.client.websocket.util.SessionMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

/**
 * Client websocket endpoint.
 */
@ClientEndpoint(encoders = {JsonEncoder.class})
public class HiveClientEndpoint implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(HiveClientEndpoint.class);
    private Session userSession;
    private MessageHandler messageHandler;
    private WebSocketContainer container;
    private SessionMonitor sessionMonitor;

    /**
     * Creates new endpoint and trying to connect to server. Client or device endpoint should be already set.
     *
     * @param endpointURI full endpoint URI (with client or device path specified).
     */
    public HiveClientEndpoint(URI endpointURI) {
        try {
            container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new InternalHiveClientException(e.getMessage(), e);
        }
    }

    /**
     * Client endpoint onOpen method. Session monitoring starts.
     *
     * @param userSession user session
     */
    @OnOpen
    public void onOpen(Session userSession) {
        this.userSession = userSession;
        sessionMonitor = new SessionMonitor(userSession);
    }

    /**
     * Client endpoint onClose method.
     *
     * @param userSession user session
     * @param reason      close reason
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        try {
            close();
        } catch (IOException e) {
            logger.debug("Dirty close.", e);
        }
        this.userSession = userSession;
    }

    /**
     * Client endpoint onMessage message. Pass message to the HiveWebsocketHandler for the further processing
     *
     * @param message message from server.
     */
    @OnMessage
    public void onMessage(String message) {
        if (messageHandler != null)
            messageHandler.handleMessage(message);
    }

    /**
     * Adds message handler
     *
     * @param msgHandler message handler
     */
    public void addMessageHandler(MessageHandler msgHandler) {
        messageHandler = msgHandler;
    }

    /**
     * Send message to the server asynchronously
     *
     * @param message message to send
     */
    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

    /**
     * Stop the session monitor
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if (sessionMonitor != null) {
            sessionMonitor.close();
        }
    }

    public static interface MessageHandler extends javax.websocket.MessageHandler {
        public void handleMessage(String message);
    }
}


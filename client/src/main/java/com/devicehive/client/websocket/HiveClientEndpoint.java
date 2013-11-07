package com.devicehive.client.websocket;


import com.devicehive.client.model.exceptions.InternalHiveClientException;
import com.devicehive.client.websocket.util.SessionMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

@ClientEndpoint(encoders = {JsonEncoder.class})
public class HiveClientEndpoint implements Closeable{
    private static final Logger logger = LoggerFactory.getLogger(HiveClientEndpoint.class);
    private Session userSession;
    private MessageHandler messageHandler;
    private WebSocketContainer container;
    private SessionMonitor sessionMonitor;

    public HiveClientEndpoint(URI endpointURI) {
        try {
            container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new InternalHiveClientException(e.getMessage(), e);
        }
    }

    @OnOpen
    public void onOpen(Session userSession) {
        this.userSession = userSession;
        sessionMonitor = new SessionMonitor(userSession);
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        this.userSession = userSession;
    }

    @OnMessage
    public void onMessage(String message) {
        if (messageHandler != null)
            messageHandler.handleMessage(message);
    }

    public void addMessageHandler(MessageHandler msgHandler) {
        messageHandler = msgHandler;
    }

    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

    @Override
    public void close() throws IOException {
        if (sessionMonitor != null){
            sessionMonitor.close();
        }
    }

    public static interface MessageHandler extends javax.websocket.MessageHandler {
        public void handleMessage(String message);
    }
}


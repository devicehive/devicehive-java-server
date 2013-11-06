package com.devicehive.client.websocket;


import com.devicehive.client.model.exceptions.InternalHiveClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.net.URI;

@ClientEndpoint(encoders = {JsonEncoder.class})
public class HiveClientEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(HiveClientEndpoint.class);
    private Session userSession;
    private MessageHandler messageHandler;
    private WebSocketContainer container;

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
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        //todo close reason?
        this.userSession = null;
    }

    @OnMessage
    public void onMessage(String message) {
        if (this.messageHandler != null)
            this.messageHandler.handleMessage(message);
    }

    public void addMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

    public static interface MessageHandler extends javax.websocket.MessageHandler {
        public void handleMessage(String message);
    }
}


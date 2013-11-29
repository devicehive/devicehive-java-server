package com.devicehive.client.websocket;


import com.devicehive.client.api.AuthenticationService;
import com.devicehive.client.context.HiveContext;
import com.devicehive.client.context.HivePrincipal;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveServerException;
import com.devicehive.client.model.exceptions.InternalHiveClientException;
import com.devicehive.client.util.connection.ConnectionEvent;
import com.devicehive.client.util.connection.HiveConnectionEventHandler;
import com.devicehive.client.websocket.util.SessionMonitor;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.Utf8DecodingError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.concurrent.*;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * Client websocket endpoint.
 */
@ClientEndpoint(encoders = {JsonEncoder.class})
public class HiveClientEndpoint implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(HiveClientEndpoint.class);
    private Session userSession;
    private MessageHandler messageHandler;
    private ClientManager clientManager;
    private SessionMonitor sessionMonitor;
    private URI endpointURI;
    private HiveContext hiveContext;
    private HiveConnectionEventHandler hiveConnectionEventHandler;

    /**
     * Creates new endpoint and trying to connect to server. Client or device endpoint should be already set.
     *
     * @param endpointURI full endpoint URI (with client or device path specified).
     */
    public HiveClientEndpoint(final URI endpointURI, HiveContext hiveContext,
                              HiveConnectionEventHandler hiveConnectionEventHandler) {
        this.endpointURI = endpointURI;
        this.hiveContext = hiveContext;
        final HiveClientEndpoint hiveClientEndpoint = this;
        this.hiveConnectionEventHandler = hiveConnectionEventHandler;
        try {
            Future<Void> future = Executors.newSingleThreadExecutor().submit(new Callable<Void>() {
                @Override
                public Void call() throws DeploymentException, IOException {
                    clientManager = ClientManager.createClient();
                    clientManager.connectToServer(hiveClientEndpoint, endpointURI);
                    return null;
                }
            });
            future.get(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("unable to establish connection! Reason: " + e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new InternalHiveClientException(e.getCause().getMessage(), e.getCause());
        } catch (TimeoutException e) {
            throw new HiveClientException("Unable to establish connection");
        }
    }

    /**
     * Client endpoint onOpen method. Session monitoring starts.
     *
     * @param userSession user session
     */
    @OnOpen
    public void onOpen(Session userSession) {
        logger.info("[onOpen] User session: {}", userSession);
        this.userSession = userSession;
        sessionMonitor = new SessionMonitor(userSession);
        hiveConnectionEventHandler.setUserSession(userSession);
    }

    /**
     * Client endpoint onClose method.
     *
     * @param userSession user session
     * @param reason      close reason
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        logger.info("[onClose] Websocket client closed. Reason: " + reason.getReasonPhrase() + "; Code: " +
                reason.getCloseCode
                        ().getCode());
        try {
            switch (reason.getCloseCode().getCode()) {  //cannot use required enum at the compile-time
                //CANNOT_ACCEPT
                case 1003:
                    reconnectToServer();
                    throw new HiveServerException(
                            "Try to reconnect on close reason: " + reason.getReasonPhrase() +
                                    "Status code: 1003",
                            INTERNAL_SERVER_ERROR.getStatusCode());

                    //CLOSED_ABNORMALLY
                case 1006:
                    logger.warn(
                            "Connection lost! Reason: " + reason.getReasonPhrase() + " Trying to reconnect..");
                    this.userSession = null;
                    while (this.userSession == null) {
                        try {
                            reconnectToServer();
                        } catch (Exception e) {
                            logger.warn("Unable to reconnect! Reason: " + e.getMessage() + "Will try again.");
                        }
                    }
                    break;

                //NOT_CONSISTENT
                case 1007:
                    reconnectToServer();
                    throw new HiveServerException(
                            "Try to reconnect on close reason: " + reason.getReasonPhrase() +
                                    "Status code: 1007",
                            INTERNAL_SERVER_ERROR.getStatusCode());

                    //TOO_BIG
                case 1009:
                    reconnectToServer();
                    throw new HiveClientException(
                            "Try to reconnect on close reason: " + reason.getReasonPhrase() +
                                    "Status code: 1009", BAD_REQUEST.getStatusCode());

                    //UNEXPECTED_CONDITION
                case 1011:
                    reconnectToServer();
                    throw new HiveServerException(
                            "Try to reconnect on close reason: " + reason.getReasonPhrase() +
                                    "Status code: 1011", BAD_REQUEST.getStatusCode());
                default:
                    try {
                        close();
                    } catch (IOException e) {
                        logger.debug("Dirty close.", e);
                    }
                    if (reason.getCloseCode().getCode() != CloseReason.CloseCodes.NORMAL_CLOSURE.getCode()) {
                        throw new InternalHiveClientException(
                                "Closed abnormally. Closure status code: " + reason
                                        .getCloseCode().getCode() + " Reason: " + reason.getReasonPhrase());
                    }
            }
        } catch (DeploymentException | IOException e) {
            throw new InternalHiveClientException(e.getMessage(), e);
        }

    }

    @OnError
    public void onError(Throwable exception) {
        logger.error("[onError] ", exception);
    }

    /**
     * Client endpoint onMessage message. Pass message to the HiveWebsocketHandler for the further processing
     *
     * @param message message from server.
     */
    @OnMessage
    public void onMessage(String message) {
        try {
            if (messageHandler != null)
                messageHandler.handleMessage(message);
        } catch (Utf8DecodingError e) {
            //decoding error occurred. exception will be thrown
            throw new InternalHiveClientException("Wrong encoding type!", e);
        }
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
        try {
            this.userSession.getAsyncRemote().sendText(message);
        } catch (Utf8DecodingError e) {
            //decoding error occurred. exception will be thrown
            throw new InternalHiveClientException("Wrong encoding type!", e);
        }
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

    private void reconnectToServer() throws IOException, DeploymentException {
        userSession = clientManager.connectToServer(this, endpointURI);
        hiveConnectionEventHandler.setUserSession(userSession);
        //need to authenticate 'cause authentication is associated with the session
        ConnectionEvent event;
        HivePrincipal principal = hiveContext.getHivePrincipal();
        if (principal == null) {
            //TODO set event for gateway
            event = new ConnectionEvent(endpointURI, null, null);
        } else {
            if (principal.getDevice() != null) {
                Pair<String, String> device = principal.getDevice();
                event = new ConnectionEvent(endpointURI, null, device.getLeft());
                AuthenticationService.authenticateDevice(device.getLeft(), device.getRight(), hiveContext);
            } else if (principal.getUser() != null) {
                Pair<String, String> user = principal.getUser();
                event = new ConnectionEvent(endpointURI, null, user.getLeft());
                AuthenticationService.authenticateClient(user.getLeft(), user.getRight(), hiveContext);
            } else {
                String key = principal.getAccessKey();
                event = new ConnectionEvent(endpointURI, null, key);
                AuthenticationService.authenticateKey(key, hiveContext);
            }
        }
        //need to resubscribe for the notifications, commands and command updates
        resubscribeForNotifications();
        resubscribeForCommands();
        checkIfCommandUpdated();
        //raise up connection event
        event.setTimestamp(new Timestamp(System.currentTimeMillis()));
        event.setLost(false);
        hiveConnectionEventHandler.handle(event);
    }

    private void resubscribeForCommands() {
        hiveContext.getHiveSubscriptions().resubscribeForCommands();
    }

    private void resubscribeForNotifications() {
        hiveContext.getHiveSubscriptions().resubscribeForNotifications();
    }

    private void checkIfCommandUpdated() {
        hiveContext.getHiveSubscriptions().requestCommandsUpdates();
    }

    public static interface MessageHandler extends javax.websocket.MessageHandler {
        public void handleMessage(String message);
    }
}


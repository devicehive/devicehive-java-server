package com.devicehive.client.websocket;


import com.devicehive.client.api.AuthenticationService;
import com.devicehive.client.context.HiveContext;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveServerException;
import com.devicehive.client.model.exceptions.InternalHiveClientException;
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

    /**
     * Creates new endpoint and trying to connect to server. Client or device endpoint should be already set.
     *
     * @param endpointURI full endpoint URI (with client or device path specified).
     */
    public HiveClientEndpoint(URI endpointURI, HiveContext hiveContext) {
        this.endpointURI = endpointURI;
        this.hiveContext = hiveContext;
        try {
            clientManager = ClientManager.createClient();
            clientManager.connectToServer(this, endpointURI);
        } catch (DeploymentException | IOException e) {
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
        logger.warn("Websocket client closed. Reason: " + reason.getReasonPhrase() + "; Code: " + reason.getCloseCode
                ().getCode());
        try {
            switch (reason.getCloseCode().getCode()) {  //cannot use required enum at the compile-time
                //CANNOT_ACCEPT
                case 1003:
                    clientManager.connectToServer(this, endpointURI);
                    throw new HiveServerException("Try to reconnect on close reason: " + reason.getReasonPhrase() +
                            "Status code: 1003",
                            INTERNAL_SERVER_ERROR.getStatusCode());

                //CLOSED_ABNORMALLY
                case 1006:
                    logger.warn("Connection lost! Reason: " + reason.getReasonPhrase() + " Trying to reconnect..");
                    this.userSession = null;
                    while (this.userSession == null) {
                        try {
                            this.userSession = clientManager.connectToServer(this, endpointURI);
                        } catch (Exception e) {
                            logger.warn("Unable to reconnect! Reason: " + e.getMessage() + "Will try again.");
                        }
                    }
                    break;

                //NOT_CONSISTENT
                case 1007:
                    clientManager.connectToServer(this, endpointURI);
                    throw new HiveServerException("Try to reconnect on close reason: " + reason.getReasonPhrase() +
                            "Status code: 1007",
                            INTERNAL_SERVER_ERROR.getStatusCode());

                //TOO_BIG
                case 1009:
                    clientManager.connectToServer(this, endpointURI);
                    throw new HiveClientException("Try to reconnect on close reason: " + reason.getReasonPhrase() +
                            "Status code: 1009", BAD_REQUEST.getStatusCode());

                //UNEXPECTED_CONDITION
                case 1011:
                    clientManager.connectToServer(this, endpointURI);
                    throw new HiveServerException("Try to reconnect on close reason: " + reason.getReasonPhrase() +
                            "Status code: 1011", BAD_REQUEST.getStatusCode());
                default:
                    try {
                        close();
                    } catch (IOException e) {
                        logger.debug("Dirty close.", e);
                    }
                    if (reason.getCloseCode().getCode() != CloseReason.CloseCodes.NORMAL_CLOSURE.getCode()) {
                        throw new InternalHiveClientException("Closed abnormally. Closure status code: " + reason
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

    public static interface MessageHandler extends javax.websocket.MessageHandler {
        public void handleMessage(String message);
    }

    private void reconnectToServer() throws IOException, DeploymentException {
        userSession = clientManager.connectToServer(this, endpointURI);
        //need to authenticate 'cause authentication is associated with the session
        if (hiveContext.getHivePrincipal().getDevice() != null){
            Pair<String, String> device = hiveContext.getHivePrincipal().getDevice();
            AuthenticationService.authenticateDevice(device.getLeft(), device.getRight(), hiveContext);
        } else if (hiveContext.getHivePrincipal().getUser() != null){
            Pair<String, String> user = hiveContext.getHivePrincipal().getUser();
            AuthenticationService.authenticateDevice(user.getLeft(), user.getRight(), hiveContext);
        } else if (hiveContext.getHivePrincipal().getAccessKey() != null){
            String key = hiveContext.getHivePrincipal().getAccessKey();
            AuthenticationService.authenticateKey(key, hiveContext);
        }
        //need to resubscribe for the notifications, commands and command updates
        resubscribeForNotifications();
        resubscribeForCommands();
        checkIfCommandUpdated();
    }

    private void resubscribeForCommands(){
       hiveContext.getHiveSubscriptions().resubscribeForCommands();
    }

    private void resubscribeForNotifications(){
       hiveContext.getHiveSubscriptions().resubscribeForNotifications();
    }

    private void checkIfCommandUpdated(){

    }
}


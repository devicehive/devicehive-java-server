package com.devicehive.client.impl.websocket;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;

/**
 * Client websocket endpoint.
 */
@ClientEndpoint(encoders = {JsonEncoder.class})
public class HiveClientEndpoint extends Endpoint {
    private static final Logger logger = LoggerFactory.getLogger(HiveClientEndpoint.class);

    /**
     * Creates new endpoint and trying to connect to server. Client or device endpoint should be already set.
     */
    public HiveClientEndpoint() {
    }

    /**
     * Client endpoint onOpen method. Session monitoring starts.
     *
     * @param userSession user session
     */
    public void onOpen(Session userSession, EndpointConfig config) {
        logger.info("[onOpen] User session: {}", userSession);
        SessionMonitor sessionMonitor = new SessionMonitor(userSession);
        userSession.getUserProperties().put(SessionMonitor.SESSION_MONITOR_KEY, sessionMonitor);
    }

    /**
     * Client endpoint onClose method.
     *
     * @param userSession user session
     * @param reason      close reason
     */
    public void onClose(Session userSession, CloseReason reason) {
        logger.info("[onClose] Websocket client closed. Reason: " + reason.getReasonPhrase() + "; Code: " +
                reason.getCloseCode
                        ().getCode());
        SessionMonitor sessionMonitor =
                (SessionMonitor) userSession.getUserProperties().get(SessionMonitor.SESSION_MONITOR_KEY);
        if (sessionMonitor != null) {
            sessionMonitor.close();
        }


                        /*
        try {
            switch (reason.getCloseCode().getCode()) {  //cannot use required enum at the compile-time
                //CANNOT_ACCEPT
                case 1003:
                    //reconnectToServer();
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
        */
    }

    public void onError(Throwable exception, Session session) {
        logger.error("[onError] ", exception);
    }

    /*
    private void reconnectToServer() throws IOException, DeploymentException {
        userSession = clientManager.connectToServer(this, endpointURI);
        hiveConnectionEventHandler.setUserSession(userSession);
        //need to authenticate 'cause authentication is associated with the session
        ConnectionEvent event;
        HivePrincipal principal = hiveContext.getHivePrincipal();
        if (principal.getDevice() != null) {
            Pair<String, String> device = principal.getDevice();
            event = new ConnectionEvent(endpointURI, null, device.getLeft());
            WebsocketAuthenticationUtil.authenticateDevice(device.getLeft(), device.getRight(), hiveContext);
        } else if (principal.getUser() != null) {
            Pair<String, String> user = principal.getUser();
            event = new ConnectionEvent(endpointURI, null, user.getLeft());
            WebsocketAuthenticationUtil.authenticateClient(user.getLeft(), user.getRight(), hiveContext);
        } else {
            String key = principal.getAccessKey();
            event = new ConnectionEvent(endpointURI, null, key);
            WebsocketAuthenticationUtil.authenticateKey(key, hiveContext);
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
    } */

}


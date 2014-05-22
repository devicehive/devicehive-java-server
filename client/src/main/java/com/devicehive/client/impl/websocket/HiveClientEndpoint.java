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

    }
    public void onError(Throwable exception, Session session) {
        logger.error("[onError] ", exception);
    }

}


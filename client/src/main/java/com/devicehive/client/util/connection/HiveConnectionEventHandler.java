package com.devicehive.client.util.connection;

import com.devicehive.client.model.exceptions.InternalHiveClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TODO
 */
public class HiveConnectionEventHandler implements ConnectionEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HiveConnectionEventHandler.class);
    private ConnectionLostNotifier connectionLostNotifier;
    private ConnectionEstablishedNotifier connectionEstablishedNotifier;
    private Session userSession;

    public HiveConnectionEventHandler(ConnectionLostNotifier connectionLostNotifier) {
        this.connectionLostNotifier = connectionLostNotifier;
    }

    public HiveConnectionEventHandler(ConnectionLostNotifier connectionLostNotifier,
                                      ConnectionEstablishedNotifier connectionEstablishedNotifier) {
        this.connectionLostNotifier = connectionLostNotifier;
        this.connectionEstablishedNotifier = connectionEstablishedNotifier;
    }

    public HiveConnectionEventHandler() {
    }

    @Override
    public void handle(final ConnectionEvent event) {
        logger.info("Connection event info. Timestamp : {}, id : {}, is lost : {}, service uri:",
                event.getTimestamp(), event.getId(), event.isLost(), event.getServiceUri());
        if (event.isLost()) {
            try {
                if (userSession != null && userSession.isOpen())
                    userSession.close(new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY,
                            "No responses from server!"));
            } catch (IOException ioe) {
                logger.debug("unable to close session");
                throw new InternalHiveClientException("Unable to close session. No pings", ioe);
            }
            //will send message in a new thread
            //what priority should have this task?
            if (connectionLostNotifier != null) {
                ExecutorService notificationSender = Executors.newSingleThreadExecutor();
                notificationSender.submit(new Runnable() {
                    @Override
                    public void run() {
                        connectionLostNotifier.notify(event.getTimestamp(), event.getId(), event.getServiceUri());
                    }
                });
            }
        }
        if (!event.isLost() && connectionEstablishedNotifier != null) {
            //will send message in a new thread
            //what priority should have this task?
            ExecutorService notificationSender = Executors.newSingleThreadExecutor();
            notificationSender.submit(new Runnable() {
                @Override
                public void run() {
                    connectionEstablishedNotifier.notify(event.getTimestamp(), event.getId(), event.getServiceUri());
                }
            });
        }
    }

    public void setUserSession(Session userSession) {
        this.userSession = userSession;
    }
}

package com.devicehive.client.impl.context.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handler for connections events delegates processing of connection lost/est
 */
public class HiveConnectionEventHandler implements ConnectionEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HiveConnectionEventHandler.class);
    private ConnectionLostNotifier connectionLostNotifier;
    private ConnectionEstablishedNotifier connectionEstablishedNotifier;

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
    public void handle(final ConnectionEvent event)  {
        logger.info("Connection event info. Timestamp : {}, id : {}, is lost : {}, service uri:",
                event.getTimestamp(), event.getId(), event.isLost(), event.getServiceUri());
        if (event.isLost()) {
            //will send message in a new thread
            //what priority should have this task?
            if (connectionLostNotifier != null) {
                ExecutorService notificationSender = Executors.newSingleThreadExecutor();
                notificationSender.submit(new Runnable() {
                    @Override
                    public void run() {
                        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
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
                    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                    connectionEstablishedNotifier.notify(event.getTimestamp(), event.getId(), event.getServiceUri());
                }
            });
        }
    }
}

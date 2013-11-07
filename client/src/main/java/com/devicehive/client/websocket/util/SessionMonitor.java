package com.devicehive.client.websocket.util;

import com.devicehive.client.config.Constants;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.client.model.exceptions.InternalHiveClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionMonitor implements Closeable {
    private static final String PONG_MESSAGE = "devicehive-client-ping";
    private static final Logger logger = LoggerFactory.getLogger(SessionMonitor.class);
    private static final Integer AWAIT_TERMINATION_TIMEOUT = 10;
    private final Date timeOfLastReceivedPong;
    private final Session userSession;
    private ScheduledExecutorService serverMonitor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService pingsSender = Executors.newSingleThreadScheduledExecutor();

    public SessionMonitor(Session userSession) {
        this.userSession = userSession;
        timeOfLastReceivedPong = new Date();
        addPongHandler();
        startMonitoring();
        sendPings();
    }

    private void addPongHandler() {
        userSession.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
            @Override
            public void onMessage(PongMessage message) {
                logger.debug("Pong received for session " + userSession.getId());
                timeOfLastReceivedPong.setTime(System.currentTimeMillis());
            }
        });
    }

    private void sendPings() {
        pingsSender.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    userSession.getAsyncRemote().sendPing(ByteBuffer.wrap(PONG_MESSAGE.getBytes()));
                } catch (IOException ioe) {
                    logger.warn("Unable to send ping", ioe);
                }
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void startMonitoring() {
        serverMonitor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - timeOfLastReceivedPong.getTime() > TimeUnit.MINUTES.toMillis
                        (Constants.WEBSOCKET_PING_TIMEOUT)) {
                    logger.info("No pings received from server for a long time. Session will be closed");
                    try {
                        userSession.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "No pings from server"));
                    } catch (IOException ioe) {
                        logger.debug("unable to close session");
                        throw new InternalHiveClientException("Unable to close session. No pings", ioe);
                    }
                    throw new HiveException("Session is dead. Try to connect once again");
                }
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void close() throws IOException {
        if (!serverMonitor.isShutdown()) {
            serverMonitor.shutdown();
        }
        pingsSender.shutdown();
        try {
            if (!pingsSender.awaitTermination(AWAIT_TERMINATION_TIMEOUT, TimeUnit.SECONDS)) {
                pingsSender.shutdownNow();
                if (!pingsSender.awaitTermination(AWAIT_TERMINATION_TIMEOUT, TimeUnit.SECONDS))
                    logger.warn("Ping sender did not terminate");
            }
        } catch (InterruptedException ie) {
            logger.warn(ie.getMessage(), ie);
            pingsSender.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
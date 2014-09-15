package com.devicehive.client.impl.websocket;

import com.devicehive.client.impl.context.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.PongMessage;
import javax.websocket.Session;

public class SessionMonitor {

    public static final String SESSION_MONITOR_KEY = "SESSION_MONITOR_KEY";
    private static final String PING_MESSAGE = "devicehive-client-ping";
    private static final Logger logger = LoggerFactory.getLogger(SessionMonitor.class);
    private final Date timeOfLastReceivedPong;
    private final Session userSession;
    private ScheduledExecutorService monitor = Executors.newScheduledThreadPool(2);

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
        monitor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().setName("pings_sender");
                    userSession.getAsyncRemote()
                        .sendPing(ByteBuffer.wrap(PING_MESSAGE.getBytes(Charset.forName("UTF-8"))));
                } catch (IOException ioe) {
                    logger.warn("Unable to send ping", ioe);
                }
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void startMonitoring() {
        monitor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("monitoring");
                if (System.currentTimeMillis() - timeOfLastReceivedPong.getTime() > TimeUnit.MINUTES.toMillis
                    (Constants.WEBSOCKET_PING_TIMEOUT)) {
                    logger.info("No pings received from server for a long time. Session will be closed");
                    try {
                        if (userSession.isOpen()) {
                            userSession.close(new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY,
                                                              "No pings from server"));
                        }
                    } catch (IOException ioe) {
                        logger.debug("unable to close session");
                    }
                }
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public void close() {
        monitor.shutdownNow();
    }

}
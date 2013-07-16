package com.devicehive.websockets.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Asynchronous;
import javax.inject.Singleton;
import javax.websocket.Session;
import java.io.IOException;

@Singleton
public class AsyncMessageDeliverer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncMessageDeliverer.class);
    private final int POOL_SIZE = 10; //TODO as parameter

    @Asynchronous
    public void deliverMessages(final Session session) throws IOException {

        boolean acquired = false;
        try {
            acquired = WebsocketSession.getCommandQueueLock(session).tryLock();
            if (acquired) {
                WebsocketSession.deliverMessages(session);
            }
        } finally {
            if (acquired) {
                WebsocketSession.getCommandQueueLock(session).unlock();
            }
        }

    }


}




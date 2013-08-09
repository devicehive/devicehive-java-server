package com.devicehive.websockets.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Singleton
public class AsyncMessageDeliverer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncMessageDeliverer.class);

    private ExecutorService executorService;


    @PostConstruct
    protected void postConstruct() {
        executorService = Executors.newCachedThreadPool();
    }

    @PreDestroy
    protected void preDestroy() {
        executorService.shutdown();
    }

    public void deliverMessages(final Session session) {

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                boolean acquired = false;
                try {
                    acquired = WebsocketSession.getQueueLock(session).tryLock();
                    if (acquired) {
                        WebsocketSession.deliverMessages(session);
                    }
                } catch (IOException e) {
                    logger.error("Can not deliver websocket message", e);
                } finally {
                    if (acquired) {
                        WebsocketSession.getQueueLock(session).unlock();
                    }
                }
            }
        });
    }


}




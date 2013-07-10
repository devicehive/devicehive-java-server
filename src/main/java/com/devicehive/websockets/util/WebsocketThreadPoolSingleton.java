package com.devicehive.websockets.util;

import javax.inject.Singleton;
import javax.websocket.Session;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class WebsocketThreadPoolSingleton {

    private final int POOL_SIZE = 10; //TODO as parameter
    private volatile ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);

    public void deliverMessagesAndNotify(final Session session) {
        pool.submit(new Callable() {
            @Override
            public Void call() throws Exception {
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
                return null;
            }
        });
    }
}

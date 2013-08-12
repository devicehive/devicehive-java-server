package com.devicehive.websockets.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static javax.ejb.ConcurrencyManagementType.BEAN;


@Singleton
@ConcurrencyManagement(BEAN)
public class AsyncMessageDeliverer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncMessageDeliverer.class);

    private static final int RETRY_COUNT = 3;
    private static final int RETRY_DELAY = 10;
    private static final int RETRY_CORE_POOL_SIZE = 10;

    private ExecutorService mainExecutorService;

    private ScheduledExecutorService retryExecutorService;




    @PostConstruct
    protected void postConstruct() {
        mainExecutorService = Executors.newCachedThreadPool();
        retryExecutorService = new ScheduledThreadPoolExecutor(RETRY_CORE_POOL_SIZE);
    }

    @PreDestroy
    protected void preDestroy() {
        mainExecutorService.shutdown();
        retryExecutorService.shutdown();
    }

    public void deliverMessages(final Session session) {

        mainExecutorService.submit(new Runnable() {

            private final AtomicInteger retryCount = new AtomicInteger(0);

            @Override
            public void run() {
                try {
                    WebsocketSession.deliverMessages(session);
                } catch (IOException ex) {
                    logger.error("Error message delivery, session id is {} ", session.getId());
                    if(retryCount.incrementAndGet() <= RETRY_COUNT) {
                        logger.info("Retry in {} seconds", RETRY_DELAY);
                        retryExecutorService.schedule(this, RETRY_DELAY, TimeUnit.SECONDS);
                    } else {
                        logger.info("No more tries");
                    }
                }
            }
        });
    }


}




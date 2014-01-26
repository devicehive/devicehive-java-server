package com.devicehive.client.impl.rest.subs;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


abstract class RestSubscription implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(RestSubscription.class);

    protected abstract void execute();

    @Override
    public final void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                execute();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}

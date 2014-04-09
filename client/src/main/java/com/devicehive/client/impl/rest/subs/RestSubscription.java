package com.devicehive.client.impl.rest.subs;


import com.devicehive.client.model.exceptions.HiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


abstract class RestSubscription implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(RestSubscription.class);

    protected static final String WAIT_TIMEOUT_PARAM = "waitTimeout";
    protected static final String FILTER_PARAM = "filter";

    protected abstract void execute() throws HiveException;

    @Override
    public final void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                execute();
            } catch (Throwable e) {
                logger.error("Error processing subscription", e);
            }
        }
    }
}

package com.devicehive.messages.handler;

import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.FutureTask;

public class RestHandlerCreator implements HandlerCreator {

    private static final Logger logger = LoggerFactory.getLogger(RestHandlerCreator.class);

    private final FutureTask<Void> futureTask;

    public RestHandlerCreator() {
        futureTask = new FutureTask<Void>(DUMMY_TASK, null);
    }

    public FutureTask<Void> getFutureTask() {
        return futureTask;
    }

    @Override
    public Runnable getHandler(final JsonElement message) {
        logger.debug("REST subscription notified");
        return futureTask;
    }

    private static final Runnable DUMMY_TASK = new Runnable() {
        @Override
        public void run() {
        }
    };
}

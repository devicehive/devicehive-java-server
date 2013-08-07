package com.devicehive.messages.handler;

import com.devicehive.messages.subscriptions.AbstractStorage;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class RestHandlerCreator implements HandlerCreator {

    private static final Logger logger = LoggerFactory.getLogger(RestHandlerCreator.class);

    private final FutureTask<Void> futureTask;

    public RestHandlerCreator(final AbstractStorage storage, final Object eventSource, final String subscriberId) {
        futureTask = new FutureTask<Void>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Unsubscribing REST: {}, {}", eventSource, subscriberId);
                storage.remove(eventSource, subscriberId);
                return null;
            }
        });
    }

    public FutureTask<Void> getFutureTask() {
        return futureTask;
    }

    @Override
    public Runnable getHandler(final JsonElement message) {
        return futureTask;
    }

}

package com.devicehive.messages.handler;

import com.google.common.util.concurrent.Runnables;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.FutureTask;

public class RestHandlerCreator implements HandlerCreator {

    private static final Logger logger = LoggerFactory.getLogger(RestHandlerCreator.class);

    private final FutureTask<Void> futureTask;

    public RestHandlerCreator() {
        futureTask = new FutureTask<Void>(Runnables.doNothing(), null);
    }

    public FutureTask<Void> getFutureTask() {
        return futureTask;
    }

    @Override
    public Runnable getHandler(final Object message, UUID sibId) {
        return futureTask;
    }

}

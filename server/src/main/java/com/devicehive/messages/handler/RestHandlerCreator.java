package com.devicehive.messages.handler;

import com.google.common.util.concurrent.Runnables;

import java.util.UUID;
import java.util.concurrent.FutureTask;

public class RestHandlerCreator<T> implements HandlerCreator<T> {

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

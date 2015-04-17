package com.devicehive.controller.util;

import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.subscriptions.AbstractStorage;
import com.devicehive.messages.subscriptions.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.*;

public class SimpleWaiter {

    private static final Logger logger = LoggerFactory.getLogger(SimpleWaiter.class);

    private static boolean waitFor(Future<Void> future, long seconds) {
        try {
            logger.debug("Waiting for {} seconds", seconds);
            future.get(seconds, TimeUnit.SECONDS);
            logger.debug("Waiting done");
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new HiveException(e.getMessage(), e, 500);
        } catch (TimeoutException e) {
            logger.debug("Waiting timeout");
            return false;
        }
    }

    public static <E, T, S extends Subscription<E, T>> boolean subscribeAndWait(AbstractStorage<E, S> storage,
                                                                                S sub,
                                                                                Future<Void> future,
                                                                                long seconds) {
        try {
            storage.insert(sub);
            return waitFor(future, seconds);
        } finally {
            storage.remove(sub);
        }
    }

    public static <E, T, S extends Subscription<E, T>> boolean subscribeAndWait(AbstractStorage<E, S> storage,
                                                                                Collection<S> subs,
                                                                                Future<Void> future,
                                                                                long seconds) {
        try {
            storage.insertAll(subs);
            return waitFor(future, seconds);
        } finally {
            logger.warn("{} subs removed", subs.size());
            storage.removeAll(subs);
        }
    }
}

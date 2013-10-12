package com.devicehive.controller.util;

import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.subscriptions.AbstractStorage;
import com.devicehive.messages.subscriptions.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimpleWaiter {

    private static final Logger logger = LoggerFactory.getLogger(SimpleWaiter.class);


    private static boolean waitFor(Future<Void> future, long seconds) {
        try {
            logger.debug("Waiting for {} seconds", seconds);
            future.get(seconds, TimeUnit.SECONDS);
            logger.debug("Waiting done");
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new HiveException("Unknown error", e, 500);
        }  catch (TimeoutException e) {
            logger.debug("Waiting timeout");
            return false;
        }
    }

    public static boolean subscribeAndWait(AbstractStorage storage, Subscription sub, Future<Void> future,
                                           long seconds) {
        try {
            storage.insert(sub);
            return waitFor(future, seconds);
        } finally {
            storage.remove(sub);
        }
    }

    public static boolean subscribeAndWait(AbstractStorage storage, Collection<? extends Subscription> subs,
                                           Future<Void> future, long seconds) {
        try {
            storage.insertAll(subs);
            return waitFor(future, seconds);
        } finally {
            storage.removeAll(subs);
        }
    }
}

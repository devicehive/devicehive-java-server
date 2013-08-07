package com.devicehive.controller;

import com.devicehive.exceptions.HiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimpleWait {

    private static final Logger logger = LoggerFactory.getLogger(SimpleWait.class);

    public static void waitFor(Future<Void> future, long seconds) {
        try {
            future.get(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new HiveException("Unknown error", e, 500);
        } catch (ExecutionException e) {
            throw new HiveException("Unknown error", e, 500);
        } catch (TimeoutException e) {
            logger.debug("Timeout");
        }
    }
}

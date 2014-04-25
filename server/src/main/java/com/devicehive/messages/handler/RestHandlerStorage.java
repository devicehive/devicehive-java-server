package com.devicehive.messages.handler;

import com.devicehive.model.DeviceCommand;
import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RestHandlerStorage {

    private static final Logger logger = LoggerFactory.getLogger(RestHandlerStorage.class);
    private static final Map<Long, SettableFuture<DeviceCommand>> restHandlers = new HashMap<>();

    private RestHandlerStorage() {
    }

    public static void setCommand(DeviceCommand dc) {
        restHandlers.get(dc.getId()).set(dc);
    }

    public static DeviceCommand getCommand(Long commandId, long timeout) {
        DeviceCommand dc = null;
        restHandlers.put(commandId, SettableFuture.<DeviceCommand>create());
        try {
            dc = restHandlers.get(commandId).get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.debug(e.getMessage(), e);
        } finally {
            restHandlers.remove(commandId);
        }
        return dc;
    }

}

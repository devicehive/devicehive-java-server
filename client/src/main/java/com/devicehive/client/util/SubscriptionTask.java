package com.devicehive.client.util;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.json.adapters.TimestampAdapter;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.google.common.reflect.TypeToken;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Callable;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;
import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT;

/**
 * Polling task.
 */
public class SubscriptionTask implements Callable<Void> {

    private static final String TIMESTAMP_PARAM = "timestamp";
    private static final String WAIT_TIMEOUT_PARAM = "waitTimeout";
    private static final String NAMES_PARAM = "names";
    private static Logger logger = LoggerFactory.getLogger(SubscriptionTask.class);
    private final HiveContext hiveContext;
    private final Integer waitTimeout;
    private final String path;
    private final Map<String, String> headers;
    private final Set<String> names;
    private final String deviceGuid;
    private Timestamp timestamp;
    private Class returnType;

    /**
     * Constructor.
     *
     * @param hiveContext hive context
     * @param timestamp   start timestamp, may be null
     * @param waitTimeout wait timeout (polling parameter)
     * @param path        notification or command poll path
     * @param headers     custom headers as path parameters, may be null
     * @param names       set of notification or command names parameters, may be null
     * @param deviceGuid  device identifier
     * @param returnType  tells command or notification will be returned in list
     */
    public SubscriptionTask(HiveContext hiveContext, Timestamp timestamp, Integer waitTimeout, String path,
                            Map<String, String> headers, Set<String> names, String deviceGuid, Class returnType) {
        this.hiveContext = ObjectUtils.cloneIfPossible(hiveContext);
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
        this.waitTimeout = ObjectUtils.cloneIfPossible(waitTimeout);
        this.path = path;
        this.headers = headers;
        this.names = names;
        this.deviceGuid = deviceGuid;
        this.returnType = returnType;
    }

    /**
     * Polling task performer. Put command or notification to the required queue.
     */
    @Override
    public Void call() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Map<String, Object> queryParams = new HashMap<>();
                    queryParams.put(TIMESTAMP_PARAM, TimestampAdapter.formatTimestamp(timestamp));
                    queryParams.put(WAIT_TIMEOUT_PARAM, waitTimeout);
                    if (names != null) {
                        queryParams.put(NAMES_PARAM, StringUtils.join(names, ","));
                    }
                    if (returnType.equals(DeviceCommand.class)) {
                        deviceCommandCase(queryParams);
                    } else {
                        deviceNotificationCase(queryParams);
                    }
                } catch (Exception e) {
                    if (e.getCause() instanceof InterruptedException) {
                        throw (InterruptedException) e.getCause();
                    } else {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        } catch (InterruptedException e) {
            logger.info(e.getMessage());
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private void deviceCommandCase(Map<String, Object> queryParams) throws InterruptedException {
        List<DeviceCommand> returned =
                hiveContext.getHiveRestClient().executeAsync(path, HttpMethod.GET, headers,
                        queryParams, null, new TypeToken<List<DeviceCommand>>() {
                }.getType(), null, COMMAND_LISTED);
        logger.debug("\n----Start Timestamp: " + timestamp + "----");
        if (returned == null) {
            returned = Collections.emptyList();
        }
        for (DeviceCommand current : returned) {
            logger.debug("id: " + current.getId() + "timestamp:" + current.getTimestamp());
        }
        if (!returned.isEmpty()) {
            for (DeviceCommand currentCommand : returned) {
                hiveContext.getCommandQueue().put(ImmutablePair.of(deviceGuid, currentCommand));
            }
            if (timestamp == null) {
                timestamp = new Timestamp(System.currentTimeMillis());
            }
            timestamp.setTime(returned.get(returned.size() - 1).getTimestamp().getTime());
        }
    }

    private void deviceNotificationCase(Map<String, Object> queryParams) throws InterruptedException {
        List<DeviceNotification> returned =
                hiveContext.getHiveRestClient().executeAsync(path, HttpMethod.GET, headers,
                        queryParams, null, new TypeToken<List<DeviceNotification>>() {
                }.getType(), null, NOTIFICATION_TO_CLIENT);
        logger.debug("\n----Start Timestamp: " + timestamp + "----");
        if (returned == null) {
            returned = Collections.emptyList();
        }
        for (DeviceNotification current : returned) {
            logger.debug("id: " + current.getId() + "timestamp:" + current.getTimestamp());
        }
        if (!returned.isEmpty()) {
            for (DeviceNotification currentNotification : returned) {
                hiveContext.getNotificationQueue().put(ImmutablePair.of(deviceGuid, currentNotification));
            }
            if (timestamp == null) {
                timestamp = new Timestamp(System.currentTimeMillis());
            }
            timestamp.setTime(returned.get(returned.size() - 1).getTimestamp().getTime());
        }
    }

}

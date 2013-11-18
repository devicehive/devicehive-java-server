package com.devicehive.client.util;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.json.adapters.TimestampAdapter;
import com.devicehive.client.model.DeviceCommand;
import com.google.common.reflect.TypeToken;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;

public class SubscriptionTask implements Callable<Void> {
    private static Logger logger = LoggerFactory.getLogger(SubscriptionTask.class);
    private final HiveContext hiveContext;
    private final Timestamp timestamp;
    private final Integer waitTimeout;
    private final String path;
    private final Map<String, String> headers;
    private final Set<String> names;
    private final String deviceGuid;


    public SubscriptionTask(HiveContext hiveContext, Timestamp timestamp, Integer waitTimeout, String path,
                            Map<String, String> headers, Set<String> names, String deviceGuid) {
        this.hiveContext = ObjectUtils.cloneIfPossible(hiveContext);
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
        this.waitTimeout = ObjectUtils.cloneIfPossible(waitTimeout);
        this.path = path;
        this.headers = headers;
        this.names = names;
        this.deviceGuid = deviceGuid;
    }

    @Override
    public Void call() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Map<String, Object> queryParams = new HashMap<>();
                queryParams.put("timestamp", TimestampAdapter.formatTimestamp(timestamp));
                queryParams.put("waitTimeout", waitTimeout);
                if (names != null) {
                    queryParams.put("names", StringUtils.join(names, ","));
                }
                List<DeviceCommand> returned =
                        hiveContext.getHiveRestClient().executeAsync(path, HttpMethod.GET, headers,
                                queryParams, null, new TypeToken<List<DeviceCommand>>() {
                        }.getType(), null, COMMAND_LISTED);
                logger.debug("\n----Start Timestamp: " + timestamp + "----");
                for (DeviceCommand current : returned) {
                    System.out.println("id: " + current.getId() + "timestamp:" + current.getTimestamp());
                }
                if (!returned.isEmpty()) {
                    for (DeviceCommand currentCommand : returned) {
                        hiveContext.getCommandQueue().put(ImmutablePair.of(deviceGuid, currentCommand));
                    }
                    timestamp.setTime(returned.get(returned.size() - 1).getTimestamp().getTime());
                }

            }
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                logger.info(e.getMessage());
                Thread.currentThread().interrupt();
            } else {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }
}

package com.devicehive.client.util;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.json.adapters.TimestampAdapter;
import com.devicehive.client.model.DeviceCommand;
import com.google.common.reflect.TypeToken;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;

public class SubscriptionTask implements Callable<Void> {
    private static Logger logger = Logger.getLogger(SubscriptionTask.class);
    private final HiveContext hiveContext;
    private final Timestamp timestamp;
    private final Integer waitTimeout;
    private final String path;
    private final Map<String, String> headers;

    public SubscriptionTask(HiveContext hiveContext, Timestamp timestamp, Integer waitTimeout, String path,
                            Map<String, String> headers) {
        this.hiveContext = ObjectUtils.cloneIfPossible(hiveContext);
        this.timestamp = timestamp;
        this.waitTimeout = waitTimeout;
        this.path = path;
        this.headers = headers;
    }

    @Override
    public Void call() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Map<String, Object> queryParams = new HashMap<>();
                queryParams.put("timestamp", TimestampAdapter.formatTimestamp(timestamp));
                queryParams.put("waitTimeout", waitTimeout);
                List<DeviceCommand> returned =
                        hiveContext.getHiveRestClient().executeAsync(path, HttpMethod.GET, headers,
                                queryParams, null, new TypeToken<List<DeviceCommand>>() {
                        }.getType(), null, COMMAND_LISTED);
                System.out.println("\n----Start Timestamp: " + timestamp + "----");
                for (DeviceCommand current : returned) {
                    System.out.println("id: " + current.getId() + "timestamp:" + current.getTimestamp());
                }
                if (!returned.isEmpty()) {
                    hiveContext.getCommandQueue().addAll(returned);
                    timestamp.setTime(returned.get(returned.size() - 1).getTimestamp().getTime());
                }
            }
        } catch (Exception e) {
            logger.error(e);
            if (e.getCause() instanceof InterruptedException)
                Thread.currentThread().interrupt();
        }
        return null;
    }
}

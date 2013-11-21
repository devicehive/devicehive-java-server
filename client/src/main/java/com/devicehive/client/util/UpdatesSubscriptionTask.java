package com.devicehive.client.util;

import com.devicehive.client.context.HiveContext;
import com.devicehive.client.model.DeviceCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;

/**
 * Command updates task performer
 */
public class UpdatesSubscriptionTask implements Callable<DeviceCommand> {
    private static Logger logger = LoggerFactory.getLogger(UpdatesSubscriptionTask.class);
    private final HiveContext hiveContext;
    private final String path;
    private final Integer waitTimeout;

    /**
     * Constructor.
     *
     * @param hiveContext hive context
     * @param path        wait path
     * @param waitTimeout wait timeout
     */
    public UpdatesSubscriptionTask(HiveContext hiveContext, String path, Integer waitTimeout) {
        this.hiveContext = hiveContext;
        this.path = path;
        this.waitTimeout = waitTimeout;
    }

    /**
     * Polling task performer. Put command updates to the required queue. Return device command that has been updated
     *
     * @return processed device command
     */
    @Override
    public DeviceCommand call() throws Exception {
        try {
            DeviceCommand returned = null;
            while (!Thread.currentThread().isInterrupted() && returned == null) {
                try {
                    Map<String, Object> queryParams = new HashMap<>();
                    queryParams.put("waitTimeout", waitTimeout);
                    returned = hiveContext.getHiveRestClient().executeAsync(path, HttpMethod.GET, null,
                            queryParams, null, DeviceCommand.class, null, COMMAND_LISTED);
                    if (returned != null) {
                        hiveContext.getCommandUpdateQueue().add(returned);
                        logger.debug("Command procceed. Id = " + returned.getId() + "Status = " + returned.getStatus());
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
            Thread.currentThread().interrupt();
            logger.info("task cancellled for path {}", path);
        }
        return null;
    }
}

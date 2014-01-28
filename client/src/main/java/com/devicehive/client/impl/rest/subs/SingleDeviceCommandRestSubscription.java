package com.devicehive.client.impl.rest.subs;


import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.json.adapters.TimestampAdapter;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.common.reflect.TypeToken;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;

public class SingleDeviceCommandRestSubscription extends RestSubscription {

    private static Logger logger = LoggerFactory.getLogger(SingleDeviceCommandRestSubscription.class);

    protected static final String TIMESTAMP_PARAM = "timestamp";
    protected static final String WAIT_TIMEOUT_PARAM = "waitTimeout";
    protected static final String NAMES_PARAM = "names";

    private HiveContext hiveContext;
    private final Integer waitTimeout;
    private final Set<String> names;
    private final String deviceGuid;
    private Timestamp timestamp;


    public SingleDeviceCommandRestSubscription(HiveContext hiveContext, Timestamp timestamp, Integer waitTimeout,
                                                Set<String> names, String deviceGuid) {
        this.hiveContext = ObjectUtils.cloneIfPossible(hiveContext);
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
        this.waitTimeout = ObjectUtils.cloneIfPossible(waitTimeout);
        this.names = names;
        this.deviceGuid = deviceGuid;
    }


    /**
     * Polling task performer. Put command or notification to the required queue.
     */
    @Override
    public void execute() {
        try {
            Map<String, Object> queryParams = new HashMap<>();
            if (timestamp != null) {
                queryParams.put(TIMESTAMP_PARAM, TimestampAdapter.formatTimestamp(timestamp));
            }
            queryParams.put(WAIT_TIMEOUT_PARAM, waitTimeout);
            if (names != null) {
                queryParams.put(NAMES_PARAM, StringUtils.join(names, ","));
            }
            String path = new StringBuilder("/device/").append(deviceGuid).append("/command/poll").toString();
            List<DeviceCommand> commands =
                    hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null,
                            queryParams, null, new TypeToken<List<DeviceCommand>>() {
                    }.getType(), null, COMMAND_LISTED);
            for (DeviceCommand command : commands) {
                Pair<String, DeviceCommand> pair = ImmutablePair.of(deviceGuid, command);
                if (timestamp == null || timestamp.before(command.getTimestamp())) {
                    timestamp = command.getTimestamp();
                }
                hiveContext.getCommandQueue().add(pair);
            }
        } catch (HiveException e) {
            logger.error(e.getMessage(), e);
        }
    }

}

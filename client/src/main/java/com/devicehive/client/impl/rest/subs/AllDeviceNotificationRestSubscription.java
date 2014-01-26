package com.devicehive.client.impl.rest.subs;


import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.json.adapters.TimestampAdapter;
import com.devicehive.client.model.CommandPollManyResponse;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.NotificationPollManyResponse;
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

public class AllDeviceNotificationRestSubscription extends RestSubscription {

    private static Logger logger = LoggerFactory.getLogger(AllDeviceNotificationRestSubscription.class);

    protected static final String TIMESTAMP_PARAM = "timestamp";
    protected static final String WAIT_TIMEOUT_PARAM = "waitTimeout";
    protected static final String NAMES_PARAM = "names";

    private HiveContext hiveContext;
    private final Integer waitTimeout;
    private final Map<String, String> headers;
    private final Set<String> names;
    private Timestamp timestamp;


    public AllDeviceNotificationRestSubscription(HiveContext hiveContext, Timestamp timestamp, Integer waitTimeout,
                                                 Map<String, String> headers, Set<String> names) {
        this.hiveContext = ObjectUtils.cloneIfPossible(hiveContext);
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
        this.waitTimeout = ObjectUtils.cloneIfPossible(waitTimeout);
        this.headers = headers;
        this.names = names;
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
            List<NotificationPollManyResponse> responses =
                    hiveContext.getHiveRestClient().execute("/device/notification/poll", HttpMethod.GET, headers,
                            queryParams, null, new TypeToken<List<CommandPollManyResponse>>() {
                    }.getType(), null, COMMAND_LISTED);
            for (NotificationPollManyResponse response : responses) {
                Pair<String, DeviceNotification> pair = ImmutablePair.of(response.getGuid(), response.getNotification());
                if (timestamp == null || timestamp.before(response.getNotification().getTimestamp())) {
                    timestamp = response.getNotification().getTimestamp();
                }
                hiveContext.getNotificationQueue().add(pair);
            }
        } catch (HiveException e) {
            logger.error(e.getMessage(), e);
        }
    }

}

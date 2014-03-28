package com.devicehive.client.impl.rest.subs;


import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.json.adapters.TimestampAdapter;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.common.reflect.TypeToken;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;

public class SingleDeviceNotificationRestSubscription extends RestSubscription {

    private static Logger logger = LoggerFactory.getLogger(SingleDeviceNotificationRestSubscription.class);

    protected static final String TIMESTAMP_PARAM = "timestamp";
    protected static final String WAIT_TIMEOUT_PARAM = "waitTimeout";
    protected static final String NAMES_PARAM = "names";

    private HiveContext hiveContext;
    private final Integer waitTimeout;
    private final Set<String> names;
    private final String deviceGuid;
    private Timestamp timestamp;


    public SingleDeviceNotificationRestSubscription(HiveContext hiveContext, Timestamp timestamp, Integer waitTimeout,
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
            String path = new StringBuilder("/device/").append(deviceGuid).append("/notification/poll").toString();
            List<DeviceNotification> notifications =
                    hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null,
                            queryParams, null, new TypeToken<List<DeviceNotification>>() {
                    }.getType(), null, COMMAND_LISTED);
            for (DeviceNotification notification : notifications) {
                if (timestamp == null || timestamp.before(notification.getTimestamp())) {
                    timestamp = notification.getTimestamp();
                }
                hiveContext.getNotificationsHandler().handle(notification);
            }
        } catch (HiveException e) {
            logger.error(e.getMessage(), e);
        }
    }

}

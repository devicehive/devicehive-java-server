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

public class CommandUpdateRestSubscription extends RestSubscription {

    private static Logger logger = LoggerFactory.getLogger(CommandUpdateRestSubscription.class);

    protected static final String WAIT_TIMEOUT_PARAM = "waitTimeout";

    private HiveContext hiveContext;
    private final Integer waitTimeout;
    private final Map<String, String> headers;
    private final Long commandId;
    private final String deviceGuid;


    public CommandUpdateRestSubscription(HiveContext hiveContext, Integer waitTimeout,
                                         Map<String, String> headers, String deviceGuid, Long commandId) {
        this.hiveContext = ObjectUtils.cloneIfPossible(hiveContext);
        this.waitTimeout = ObjectUtils.cloneIfPossible(waitTimeout);
        this.commandId = commandId;
        this.deviceGuid = deviceGuid;
        this.headers = headers;
    }


    /**
     * Polling task performer. Put command or notification to the required queue.
     */
    @Override
    public void execute() {
        try {
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put(WAIT_TIMEOUT_PARAM, waitTimeout);
            String path = new StringBuilder("/device/").append(deviceGuid).append("/command/").append(commandId).append("poll").toString();
            DeviceCommand command =
                    hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, headers,
                            queryParams, null, new TypeToken<DeviceCommand>() {
                    }.getType(), null, COMMAND_LISTED);
            if (command != null) {
                hiveContext.getCommandUpdateQueue().add(command);
                Thread.currentThread().interrupt();
            }
        } catch (HiveException e) {
            logger.error(e.getMessage(), e);
        }
    }

}

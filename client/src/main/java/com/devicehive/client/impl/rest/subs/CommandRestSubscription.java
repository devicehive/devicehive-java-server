package com.devicehive.client.impl.rest.subs;


import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.impl.json.adapters.TimestampAdapter;
import com.devicehive.client.model.CommandPollManyResponse;
import com.devicehive.client.model.SubscriptionFilter;
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

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;

public class CommandRestSubscription extends RestSubscription {

    private static Logger logger = LoggerFactory.getLogger(CommandRestSubscription.class);

    private HiveContext hiveContext;
    private final int waitTimeout;
    private final SubscriptionFilter filter;


    public CommandRestSubscription(HiveContext hiveContext, SubscriptionFilter filter, int waitTimeout) {
        this.hiveContext = hiveContext;
        this.waitTimeout = waitTimeout;
        this.filter = ObjectUtils.cloneIfPossible(filter);
    }


    /**
     * Polling task performer. Put command or notification to the required queue.
     */
    @Override
    public void execute() throws HiveException {

        Map<String, String> formParams = new HashMap<>();
        formParams.put(WAIT_TIMEOUT_PARAM, String.valueOf(waitTimeout));
        formParams.put(FILTER_PARAM, GsonFactory.createGson().toJson(filter));

        @SuppressWarnings("SerializableHasSerializationMethods")
        List<CommandPollManyResponse> responses =
                hiveContext.getHiveRestClient().executeForm("/device/command/poll", formParams, new TypeToken<List<CommandPollManyResponse>>() {
                }.getType(), COMMAND_LISTED);
        for (CommandPollManyResponse response : responses) {
            Timestamp timestamp = filter.getTimestamp();
            if (timestamp == null || timestamp.before(response.getCommand().getTimestamp())) {
                filter.setTimestamp(response.getCommand().getTimestamp());
            }
            hiveContext.getCommandsHandler().handleCommandInsert(response.getCommand());
        }

    }

}

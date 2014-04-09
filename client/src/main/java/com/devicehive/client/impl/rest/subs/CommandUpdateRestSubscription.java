package com.devicehive.client.impl.rest.subs;


import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.common.reflect.TypeToken;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.Map;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;

public class CommandUpdateRestSubscription extends RestSubscription {

    protected static final String WAIT_TIMEOUT_PARAM = "waitTimeout";
    private static Logger logger = LoggerFactory.getLogger(CommandUpdateRestSubscription.class);
    private final Integer waitTimeout;
    private final Long commandId;
    private final String deviceGuid;
    private HiveContext hiveContext;


    public CommandUpdateRestSubscription(HiveContext hiveContext, Integer waitTimeout, String deviceGuid,
                                         Long commandId) {
        this.hiveContext = hiveContext;
        this.waitTimeout = ObjectUtils.cloneIfPossible(waitTimeout);
        this.commandId = commandId;
        this.deviceGuid = deviceGuid;
    }

    /**
     * Polling task performer. Put command or notification to the required queue.
     */
    @Override
    public void execute() {
        try {
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put(WAIT_TIMEOUT_PARAM, waitTimeout);
            String path = new StringBuilder("/device/").append(deviceGuid).append("/command/").append(commandId)
                    .append("/poll").toString();
            @SuppressWarnings("SerializableHasSerializationMethods")
            DeviceCommand command =
                    hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null,
                            queryParams, null, new TypeToken<DeviceCommand>() {
                    }.getType(), null, COMMAND_LISTED);
            if (command != null) {
                hiveContext.getCommandsHandler().handleCommandUpdate(command);
                Thread.currentThread().interrupt();
            }
        } catch (HiveException e) {
            logger.error(e.getMessage(), e);
        }
    }

}

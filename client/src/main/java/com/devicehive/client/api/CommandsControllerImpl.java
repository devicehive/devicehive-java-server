package com.devicehive.client.api;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.model.DeviceCommand;
import com.google.common.reflect.TypeToken;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class CommandsControllerImpl implements CommandsController {

    private final HiveContext hiveContext;

    public CommandsControllerImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    @Override
    public List<DeviceCommand> queryCommands(String deviceGuid, Timestamp start, Timestamp end, String commandName,
                                             String status, String sortField, String sortOrder, Integer take,
                                             Integer skip) {
        String path = "/device/" + deviceGuid + "/command";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("start", start);
        queryParams.put("end", end);
        queryParams.put("command", commandName);
        queryParams.put("status", status);
        queryParams.put("sortField", sortField);
        queryParams.put("sortOrder", sortOrder);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, queryParams,
                new TypeToken<List<DeviceCommand>>() {
                }.getType(), COMMAND_LISTED);
    }

    @Override
    public DeviceCommand getCommand(String guid, long id) {
        String path = "/device/" + guid + "/command/" + id;
        return hiveContext.getHiveRestClient()
                .execute(path, HttpMethod.GET, null, DeviceCommand.class, COMMAND_TO_DEVICE);
    }

    @Override
    public DeviceCommand insertCommand(String guid, DeviceCommand command) {
        String path = "/device/" + guid + "/command";
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, command,
                DeviceCommand.class, COMMAND_FROM_CLIENT, COMMAND_TO_CLIENT);
    }

    @Override
    public void updateCommand(String deviceGuid, long id, DeviceCommand command) {
        String path = "/device/" + deviceGuid + "/command/" + id;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, command, REST_COMMAND_UPDATE_FROM_DEVICE);
    }

    @Override
    public void subscribeForCommands(Timestamp timestamp, String ... deviceIds) {
        hiveContext.addCommandsSubscription(null, timestamp, deviceIds);
    }

    @Override
    public void unsubscribeFromCommands(String ... deviceIds) {
        hiveContext.removeCommandSubscription(deviceIds);
    }
}

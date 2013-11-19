package com.devicehive.client.api.client;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.json.GsonFactory;
import com.devicehive.client.json.adapters.TimestampAdapter;
import com.devicehive.client.model.DeviceCommand;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.*;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class CommandsControllerImpl implements CommandsController {

    private static Logger logger = LoggerFactory.getLogger(CommandsController.class);
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
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "command/insert");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            request.addProperty("deviceGuid", guid);
            Gson gson = GsonFactory.createGson(COMMAND_FROM_CLIENT);
            request.add("command", gson.toJsonTree(command));
            return hiveContext.getHiveWebSocketClient().sendMessage(request, "command", DeviceCommand.class,
                    COMMAND_TO_CLIENT);
        } else {
            String path = "/device/" + guid + "/command";
            DeviceCommand proceed = hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, command,
                    DeviceCommand.class, COMMAND_FROM_CLIENT, COMMAND_TO_CLIENT);
            hiveContext.getHiveSubscriptions().addCommandUpdateSubscription(proceed.getId(), guid);
            return proceed;
        }
    }

    @Override
    public void updateCommand(String deviceGuid, long id, DeviceCommand command) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "command/update");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            request.addProperty("deviceGuid", deviceGuid);
            request.addProperty("commandId", command.getId());
            Gson gson = GsonFactory.createGson(COMMAND_UPDATE_FROM_DEVICE);
            request.add("command", gson.toJsonTree(command));
            hiveContext.getHiveWebSocketClient().sendMessage(request);
        } else {
            String path = "/device/" + deviceGuid + "/command/" + id;
            hiveContext.getHiveRestClient()
                    .execute(path, HttpMethod.PUT, null, command, REST_COMMAND_UPDATE_FROM_DEVICE);
            System.out.print("Try to update");
        }
    }

    @Override
    public void subscribeForCommands(Timestamp timestamp, Set<String> names, String... deviceIds) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "command/subscribe");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            request.addProperty("timestamp", TimestampAdapter.formatTimestamp(timestamp));
            Gson gson = GsonFactory.createGson();
            request.add("deviceGuids", gson.toJsonTree(deviceIds));
            request.add("names", gson.toJsonTree(names));
            hiveContext.getHiveWebSocketClient().sendMessage(request);
        } else {
            hiveContext.getHiveSubscriptions().addCommandsSubscription(null, timestamp, names, deviceIds);
        }
    }

    @Override
    public void unsubscribeFromCommands(Set<String> names, String... deviceIds) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "command/unsubscribe");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            Gson gson = GsonFactory.createGson();
            request.add("deviceGuids", gson.toJsonTree(deviceIds));
            hiveContext.getHiveWebSocketClient().sendMessage(request);
        } else {
            hiveContext.getHiveSubscriptions().removeCommandSubscription(names, deviceIds);
        }
    }

    @Override
    public Queue<Pair<String, DeviceCommand>> getCommandQueue() {
        return hiveContext.getCommandQueue();
    }

    @Override
    public Queue<DeviceCommand> getCommandUpdatesQueue() {
        return hiveContext.getCommandUpdateQueue();
    }
}

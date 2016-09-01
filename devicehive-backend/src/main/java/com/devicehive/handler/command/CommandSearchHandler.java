package com.devicehive.handler.command;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.rpc.CommandSearchRequest;
import com.devicehive.model.rpc.CommandSearchResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CommandSearchHandler implements RequestHandler {

    @Autowired
    private HazelcastService hazelcastService;

    @Override
    public Response handle(Request request) {
        CommandSearchRequest searchRequest = (CommandSearchRequest) request.getBody();

        CommandSearchResponse payload = searchRequest.getId() != null && !StringUtils.isEmpty(searchRequest.getGuid())
                ? searchSingleCommandByDeviceAndId(searchRequest.getId(), searchRequest.getGuid())
                : searchMultipleCommands(searchRequest);

        return Response.newBuilder()
                .withBody(payload)
                .buildSuccess();
    }

    private CommandSearchResponse searchSingleCommandByDeviceAndId(long id, String guid) {
        final CommandSearchResponse commandSearchResponse = new CommandSearchResponse();
        final List<DeviceCommand> commands = hazelcastService.find(id, guid, DeviceCommand.class)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());

        commandSearchResponse.setCommands(commands);
        return commandSearchResponse;
    }

    private CommandSearchResponse searchMultipleCommands(CommandSearchRequest searchRequest) {
        final CommandSearchResponse commandSearchResponse = new CommandSearchResponse();
        final Collection<DeviceCommand> commands = hazelcastService.find(
                searchRequest.getDevices(),
                searchRequest.getNames(),
                searchRequest.getTimestamp(),
                searchRequest.getStatus(),
                searchRequest.getTake(),
                searchRequest.getHasResponse(),
                DeviceCommand.class);

        commandSearchResponse.setCommands(new ArrayList<>(commands));
        return commandSearchResponse;
    }
}

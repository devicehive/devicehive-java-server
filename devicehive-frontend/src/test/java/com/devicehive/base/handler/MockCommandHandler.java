package com.devicehive.base.handler;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.rpc.*;
import com.devicehive.shim.api.Body;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.server.RequestHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class MockCommandHandler {

    private Set<DeviceCommand> commandSet = new HashSet<>();

    public void handle (RequestHandler handler) {
        when(handler.handle(any(Request.class))).thenAnswer(invocation -> {
            Request request = invocation.getArgumentAt(0, Request.class);
            if (request.getBody() instanceof CommandInsertRequest) {
                DeviceCommand command = ((CommandInsertRequest) request.getBody()).getDeviceCommand();

                commandSet.add(command);

                CommandInsertResponse payload = new CommandInsertResponse(command);
                return com.devicehive.shim.api.Response.newBuilder()
                        .withBody(payload)
                        .buildSuccess();
            } else if (request.getBody() instanceof CommandUpdateRequest) {
                final DeviceCommand command = request.getBody().cast(CommandUpdateRequest.class).getDeviceCommand();

                commandSet.add(command);

                return com.devicehive.shim.api.Response.newBuilder().buildSuccess();
            } else if (request.getBody() instanceof CommandSubscribeRequest) {
                CommandSubscribeRequest body = (CommandSubscribeRequest) request.getBody();
                Set<DeviceCommand> commands = commandSet
                        .stream()
                        .filter(n -> n.getDeviceGuid().equals(body.getDevice()))
                        .collect(Collectors.toSet());
                CommandSubscribeResponse subscribeResponse = new CommandSubscribeResponse(body.getSubscriptionId(), commands);

                return com.devicehive.shim.api.Response.newBuilder()
                        .withBody(subscribeResponse)
                        .withLast(false)
                        .withCorrelationId(request.getCorrelationId())
                        .buildSuccess();
            } else if (request.getBody() instanceof CommandSearchRequest) {
                CommandSearchRequest body = (CommandSearchRequest) request.getBody();

                final CommandSearchResponse commandSearchResponse = new CommandSearchResponse();
                final List<DeviceCommand> commands =  commandSet
                        .stream()
                        .filter(command -> command.getId().equals(body.getId()) || command.getDeviceGuid().equals(body.getGuid()))
                        .collect(Collectors.toList());

                commandSearchResponse.setCommands(commands);

                return com.devicehive.shim.api.Response.newBuilder()
                        .withBody(commandSearchResponse)
                        .buildSuccess();
            } else if (request.getBody() instanceof CommandUpdateSubscribeRequest) {
                final CommandUpdateSubscribeRequest body = request.getBody().cast(CommandUpdateSubscribeRequest.class);
                final DeviceCommand deviceCommand = commandSet
                        .stream()
                        .filter(command -> command.getDeviceGuid().equals(body.getGuid()))
                        .findFirst()
                        .get();

                return com.devicehive.shim.api.Response.newBuilder()
                        .withBody(new CommandUpdateSubscribeResponse(body.getSubscriptionId(), deviceCommand))
                        .withLast(false)
                        .withCorrelationId(request.getCorrelationId())
                        .buildSuccess();

            } else {
                return com.devicehive.shim.api.Response.newBuilder()
                        .withBody(new Body("") {
                        })
                        .buildSuccess();
            }

        });
    }
}

package com.devicehive.base.handler;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.rpc.*;
import com.devicehive.shim.api.Body;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class MockCommandHandler {

    private Map<Long, DeviceCommand> commandMap = new HashMap<>();

    public void handle (RequestHandler handler) {
        when(handler.handle(any(Request.class))).thenAnswer(invocation -> {
            Request request = invocation.getArgumentAt(0, Request.class);
            if (request.getBody() instanceof CommandInsertRequest) {
                DeviceCommand command = ((CommandInsertRequest) request.getBody()).getDeviceCommand();

                commandMap.put(command.getId(), command);

                CommandInsertResponse payload = new CommandInsertResponse(command);
                return com.devicehive.shim.api.Response.newBuilder()
                        .withBody(payload)
                        .buildSuccess();
            } else if (request.getBody() instanceof CommandUpdateRequest) {
                final DeviceCommand command = request.getBody().cast(CommandUpdateRequest.class).getDeviceCommand();

                commandMap.put(command.getId(), command);

                return com.devicehive.shim.api.Response.newBuilder().buildSuccess();
            } else if (request.getBody() instanceof CommandSubscribeRequest) {
                CommandSubscribeRequest body = (CommandSubscribeRequest) request.getBody();
                Set<DeviceCommand> commands = commandMap.values()
                        .stream()
                        .filter(n -> n.getDeviceId().equals(body.getDevice()))
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
                final List<DeviceCommand> commands =  commandMap.values()
                        .stream()
                        .filter(command -> command.getId().equals(body.getId()) || command.getDeviceId().equals(body.getDeviceId()))
                        .collect(Collectors.toList());

                commandSearchResponse.setCommands(commands);

                return com.devicehive.shim.api.Response.newBuilder()
                        .withBody(commandSearchResponse)
                        .buildSuccess();
            } else if (request.getBody() instanceof CommandUpdateSubscribeRequest) {
                final CommandUpdateSubscribeRequest body = request.getBody().cast(CommandUpdateSubscribeRequest.class);
                final DeviceCommand deviceCommand = commandMap.values()
                        .stream()
                        .filter(command -> command.getDeviceId().equals(body.getDeviceId()))
                        .findFirst()
                        .get();

                return com.devicehive.shim.api.Response.newBuilder()
                        .withBody(new CommandUpdateSubscribeResponse(body.getSubscriptionId(), deviceCommand))
                        .withLast(false)
                        .withCorrelationId(request.getCorrelationId())
                        .buildSuccess();

            } else if (request.getBody() instanceof CommandUnsubscribeRequest) {
                CommandUnsubscribeRequest body = (CommandUnsubscribeRequest) request.getBody();
                CommandUnsubscribeResponse unsubscribeResponse = new CommandUnsubscribeResponse(body.getSubscriptionId(), null);

                return Response.newBuilder()
                        .withBody(unsubscribeResponse)
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

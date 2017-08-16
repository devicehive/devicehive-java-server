package com.devicehive.handler.command;

/*
 * #%L
 * DeviceHive Backend Logic
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

import com.devicehive.eventbus.EventBus;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.shim.api.Action;
import com.devicehive.model.rpc.CommandUpdateSubscribeRequest;
import com.devicehive.model.rpc.CommandUpdateSubscribeResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;

public class CommandUpdateSubscribeRequestHandler implements RequestHandler {

    @Autowired
    private HazelcastService hazelcastService;

    @Autowired
    private EventBus eventBus;

    @Override
    public Response handle(Request request) {
        final CommandUpdateSubscribeRequest body = request.getBody().cast(CommandUpdateSubscribeRequest.class);

        final Subscriber subscriber = new Subscriber(body.getSubscriptionId(), request.getReplyTo(), request.getCorrelationId());
        final Subscription subscription = new Subscription(Action.COMMAND_UPDATE_EVENT.name(), Long.toString(body.getCommandId()));

        eventBus.subscribe(subscriber, subscription);

        final DeviceCommand deviceCommand = hazelcastService
                .find(body.getCommandId(), body.getDeviceId(), DeviceCommand.class)
                .orElse(null);

        return Response.newBuilder()
                .withBody(new CommandUpdateSubscribeResponse(body.getSubscriptionId(), deviceCommand))
                .withLast(false)
                .withCorrelationId(request.getCorrelationId())
                .buildSuccess();
    }
}

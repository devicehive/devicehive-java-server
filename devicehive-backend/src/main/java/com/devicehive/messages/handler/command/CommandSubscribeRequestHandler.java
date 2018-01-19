package com.devicehive.messages.handler.command;

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
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.rpc.CommandSubscribeRequest;
import com.devicehive.model.rpc.CommandSubscribeResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;

import static com.devicehive.shim.api.Action.COMMANDS_UPDATE_EVENT;

@Component
public class CommandSubscribeRequestHandler implements RequestHandler {

    private EventBus eventBus;
    private HazelcastService hazelcastService;

    @Autowired
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Autowired
    public void setHazelcastService(HazelcastService hazelcastService) {
        this.hazelcastService = hazelcastService;
    }

    @Override
    public Response handle(Request request) {
        CommandSubscribeRequest body = (CommandSubscribeRequest) request.getBody();
        validate(body);

        Subscriber subscriber = new Subscriber(body.getSubscriptionId(), request.getReplyTo(), request.getCorrelationId());
        Filter filter = body.getFilter();

        if (body.isReturnUpdated()) {
            filter.setEventName(COMMANDS_UPDATE_EVENT.name());
        }
        eventBus.subscribe(filter, subscriber);

        Collection<DeviceCommand> commands = findCommands(filter, body.getNames(), body.getTimestamp(), body.isReturnUpdated(), body.getLimit());
        CommandSubscribeResponse subscribeResponse = new CommandSubscribeResponse(body.getSubscriptionId(), commands);

        return Response.newBuilder()
                .withBody(subscribeResponse)
                .withLast(false)
                .withCorrelationId(request.getCorrelationId())
                .buildSuccess();
    }

    private void validate(CommandSubscribeRequest request) {
        Assert.notNull(request, "Request body is null");
        Assert.notNull(request.getFilter(), "Filter is null");
        Assert.notNull(request.getSubscriptionId(), "Subscription id not provided");
    }

    private Collection<DeviceCommand> findCommands(Filter filter, Collection<String> names, Date timestamp, boolean returnUpdated, Integer limit) {
        return Optional.ofNullable(timestamp)
                .map(t -> hazelcastService.find(filter.getDeviceId(),
                        Collections.singleton(filter.getNetworkId()),
                        Collections.singleton(filter.getDeviceTypeId()),
                        names, limit, t, null, returnUpdated, null, DeviceCommand.class))
                .orElse(Collections.emptyList());
    }
}

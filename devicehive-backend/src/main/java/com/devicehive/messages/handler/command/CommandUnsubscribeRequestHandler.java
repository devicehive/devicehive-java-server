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
import com.devicehive.eventbus.FilterRegistry;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.rpc.CommandUnsubscribeRequest;
import com.devicehive.model.rpc.CommandUnsubscribeResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class CommandUnsubscribeRequestHandler implements RequestHandler {

    private EventBus eventBus;
    private FilterRegistry filterRegistry;

    @Autowired
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Autowired
    public void setFilterRegistry(FilterRegistry filterRegistry) {
        this.filterRegistry = filterRegistry;
    }

    @Override
    public Response handle(Request request) {
        CommandUnsubscribeRequest body = (CommandUnsubscribeRequest) request.getBody();
        validate(body);

        if (body.getSubscriptionIds() != null) {
            for (Long subId : body.getSubscriptionIds()) {
                Subscriber subscriber = new Subscriber(subId, request.getReplyTo(), request.getCorrelationId());
                eventBus.unsubscribe(subscriber);
                filterRegistry.unregister(subId);
            }

            CommandUnsubscribeResponse unsubscribeResponse = new CommandUnsubscribeResponse(body.getSubscriptionIds());

            return Response.newBuilder()
                    .withBody(unsubscribeResponse)
                    .withCorrelationId(request.getCorrelationId())
                    .buildSuccess();
        } else {
            throw new IllegalArgumentException("Subscription ids are null");
        }
    }

    private void validate(CommandUnsubscribeRequest request) {
        Assert.notNull(request, "Request body is null");
    }

}

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
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.CommandGetSubscriptionRequest;
import com.devicehive.model.rpc.CommandGetSubscriptionResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CommandGetSubscriptionRequestHandler implements RequestHandler {

    private EventBus eventBus;

    @Autowired
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public Response handle(Request request) {
        CommandGetSubscriptionRequest body = (CommandGetSubscriptionRequest) request.getBody();

        Subscriber subscriber = new Subscriber(body.getSubscriptionId(), request.getReplyTo(), request.getCorrelationId());

        Set<Subscription> subscribers = (Set<Subscription>) eventBus.getSubscriptions(subscriber);

        CommandGetSubscriptionResponse getSubscriptionResponse = new CommandGetSubscriptionResponse(subscribers);

        return Response.newBuilder()
                .withBody(getSubscriptionResponse)
                .withCorrelationId(request.getCorrelationId())
                .buildSuccess();
    }
}

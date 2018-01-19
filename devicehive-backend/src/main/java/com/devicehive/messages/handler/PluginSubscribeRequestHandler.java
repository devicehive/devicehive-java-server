package com.devicehive.messages.handler;

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

import com.devicehive.messages.handler.command.CommandSubscribeRequestHandler;
import com.devicehive.messages.handler.notification.NotificationSubscribeRequestHandler;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.rpc.*;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;

import static com.devicehive.shim.api.Action.COMMAND_EVENT;
import static com.devicehive.shim.api.Action.NOTIFICATION_EVENT;
import static java.util.stream.Collectors.toList;

@Component
public class PluginSubscribeRequestHandler implements RequestHandler {

    private CommandSubscribeRequestHandler commandSubscribeRequestHandler;
    private NotificationSubscribeRequestHandler notificationSubscribeRequestHandler;
    @Autowired
    public void setCommandSubscribeRequestHandler(CommandSubscribeRequestHandler commandSubscribeRequestHandler) {
        this.commandSubscribeRequestHandler = commandSubscribeRequestHandler;
    }

    @Autowired
    public void setNotificationSubscribeRequestHandler(NotificationSubscribeRequestHandler notificationSubscribeRequestHandler) {
        this.notificationSubscribeRequestHandler = notificationSubscribeRequestHandler;
    }

    @Override
    public Response handle(Request request) {
        PluginSubscribeRequest body = (PluginSubscribeRequest) request.getBody();
        validate(body);
        
        if (body.isReturnCommands()) {
            createCommandSubscription(body, false);
        }
        if (body.isReturnUpdatedCommands()) {
            createCommandSubscription(body, true);
        }
        if (body.isReturnNotifications()) {
            createNotificationSubscription(body);
        }

        return Response.newBuilder()
                .withBody(new PluginSubscribeResponse(body.getSubscriptionId()))
                .withLast(false)
                .withCorrelationId(request.getCorrelationId())
                .buildSuccess();
    }

    private List<Response> createNotificationSubscription(PluginSubscribeRequest body) {
         
        return body.getFilters().stream()
                .map(filter -> {
                    filter.setEventName(NOTIFICATION_EVENT.name());
                    NotificationSubscribeRequest notificationSubscribeRequest = new NotificationSubscribeRequest(
                            body.getSubscriptionId(), filter, body.getNames(), null);

                    Request notificationRequest = Request.newBuilder()
                            .withBody(notificationSubscribeRequest)
                            .withSingleReply(false)
                            .build();
                    notificationRequest.setReplyTo(body.getTopicName());
                    return notificationSubscribeRequestHandler.handle(notificationRequest);
                }).collect(toList());
    }

    private List<Response> createCommandSubscription(PluginSubscribeRequest body, boolean returnUpdated) {

        return body.getFilters().stream()
                .map(filter -> {
                    filter.setEventName(COMMAND_EVENT.name());
                    CommandSubscribeRequest commandSubscribeRequest = new CommandSubscribeRequest(body.getSubscriptionId(),
                            filter, body.getNames(), null, returnUpdated, 0);

                    Request commandRequest = Request.newBuilder()
                            .withBody(commandSubscribeRequest)
                            .withSingleReply(false)
                            .build();
                    commandRequest.setReplyTo(body.getTopicName());
                    return commandSubscribeRequestHandler.handle(commandRequest);
                }).collect(toList());
    }

    private void validate(PluginSubscribeRequest request) {
        Assert.notNull(request, "Request body is null");
        Assert.notNull(request.getFilters(), "Filters is null");
        Assert.notNull(request.getSubscriptionId(), "Subscription id not provided");
    }

}
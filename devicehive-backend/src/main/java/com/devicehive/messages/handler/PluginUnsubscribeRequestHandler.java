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

import com.devicehive.messages.handler.command.CommandUnsubscribeRequestHandler;
import com.devicehive.messages.handler.notification.NotificationUnsubscribeRequestHandler;
import com.devicehive.model.rpc.CommandUnsubscribeRequest;
import com.devicehive.model.rpc.NotificationUnsubscribeRequest;
import com.devicehive.model.rpc.PluginUnsubscribeRequest;
import com.devicehive.model.rpc.PluginUnsubscribeResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;


@Component
public class PluginUnsubscribeRequestHandler implements RequestHandler {

    private CommandUnsubscribeRequestHandler commandUnsubscribeRequestHandler;
    private NotificationUnsubscribeRequestHandler notificationUnsubscribeRequestHandler;

    @Autowired
    public void setCommandSubscribeRequestHandler(CommandUnsubscribeRequestHandler commandUnsubscribeRequestHandler) {
        this.commandUnsubscribeRequestHandler = commandUnsubscribeRequestHandler;
    }

    @Autowired
    public void setNotificationSubscribeRequestHandler(NotificationUnsubscribeRequestHandler notificationUnsubscribeRequestHandler) {
        this.notificationUnsubscribeRequestHandler = notificationUnsubscribeRequestHandler;
    }

    @Override
    public Response handle(Request request) {
        PluginUnsubscribeRequest body = (PluginUnsubscribeRequest) request.getBody();
        validate(body);
        
        removeCommandSubscription(body);
        removeNotificationSubscription(body);
        
        return Response.newBuilder()
                .withBody(new PluginUnsubscribeResponse(body.getSubscriptionId()))
                .withLast(false)
                .withCorrelationId(request.getCorrelationId())
                .buildSuccess();
    }

    private Response removeNotificationSubscription(PluginUnsubscribeRequest body) {
        NotificationUnsubscribeRequest notificationUnsubscribeRequest = 
                new NotificationUnsubscribeRequest(ImmutableSet.of(body.getSubscriptionId()));
    
        Request notificationRequest = Request.newBuilder()
                .withBody(notificationUnsubscribeRequest)
                .withSingleReply(false)
                .build();
        notificationRequest.setReplyTo(body.getTopicName());
        return notificationUnsubscribeRequestHandler.handle(notificationRequest);
    }

    private Response removeCommandSubscription(PluginUnsubscribeRequest body) {
         
        CommandUnsubscribeRequest commandUnsubscribeRequest = 
                new CommandUnsubscribeRequest(ImmutableSet.of(body.getSubscriptionId()));
        
        Request commandRequest = Request.newBuilder()
                .withBody(commandUnsubscribeRequest)
                .withSingleReply(false)
                .build();
        commandRequest.setReplyTo(body.getTopicName());
        return commandUnsubscribeRequestHandler.handle(commandRequest);    
    }

    private void validate(PluginUnsubscribeRequest request) {
        Assert.notNull(request, "Request body is null");
        Assert.notNull(request.getSubscriptionId(), "Subscription id not provided");
    }

}
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
import com.devicehive.model.rpc.CommandSubscribeRequest;
import com.devicehive.model.rpc.NotificationSubscribeRequest;
import com.devicehive.model.rpc.PluginSubscribeRequest;
import com.devicehive.model.rpc.PluginSubscribeResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;

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
         
//        return body.getFilter().getDeviceIds().stream() fixme: redesign it
//                .map(deviceId -> {
//                    NotificationSubscribeRequest notificationSubscribeRequest = new NotificationSubscribeRequest(
//                            body.getSubscriptionId(), deviceId, body.getFilter(), body.getTimestamp());
//
//                    Request notificationRequest = Request.newBuilder()
//                            .withBody(notificationSubscribeRequest)
//                            .withSingleReply(false)
//                            .build();
//                    notificationRequest.setReplyTo(body.getTopicName());
//                    return notificationSubscribeRequestHandler.handle(notificationRequest);
//                }).collect(toList());
        return Collections.emptyList();
    }

    private List<Response> createCommandSubscription(PluginSubscribeRequest body, boolean returnUpdated) {

//        return body.getFilter().getDeviceIds().stream() fixme: redesign it
//                .map(deviceId -> {
//                    CommandSubscribeRequest commandSubscribeRequest = new CommandSubscribeRequest(body.getSubscriptionId(),
//                            deviceId, body.getFilter(), body.getTimestamp(), returnUpdated, 0);
//
//                    Request commandRequest = Request.newBuilder()
//                            .withBody(commandSubscribeRequest)
//                            .withSingleReply(false)
//                            .build();
//                    commandRequest.setReplyTo(body.getTopicName());
//                    return commandSubscribeRequestHandler.handle(commandRequest);
//                }).collect(toList());
        return Collections.emptyList();
    }

    private void validate(PluginSubscribeRequest request) {
        Assert.notNull(request, "Request body is null");
        Assert.notNull(request.getFilter(), "Filter is null");
        Assert.notNull(request.getSubscriptionId(), "Subscription id not provided");
    }

}
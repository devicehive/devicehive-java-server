package com.devicehive.messages.handler.notification;

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
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.eventbus.events.NotificationEvent;
import com.devicehive.model.rpc.NotificationInsertRequest;
import com.devicehive.model.rpc.NotificationInsertResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationInsertHandler implements RequestHandler {

    private HazelcastService hazelcastService;
    private EventBus eventBus;

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
        DeviceNotification notification = ((NotificationInsertRequest) request.getBody()).getDeviceNotification();
        NotificationEvent notificationEvent = new NotificationEvent(notification);

        eventBus.publish(notificationEvent);
        hazelcastService.store(notification);

        NotificationInsertResponse payload = new NotificationInsertResponse(notification);
        return Response.newBuilder()
                .withBody(payload)
                .buildSuccess();
    }
}

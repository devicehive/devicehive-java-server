package com.devicehive.handler.notification;

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

import com.devicehive.model.DeviceNotification;
import com.devicehive.model.rpc.NotificationSearchRequest;
import com.devicehive.model.rpc.NotificationSearchResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class NotificationSearchHandler implements RequestHandler {

    @Autowired
    private HazelcastService storageService;

    @Override
    public Response handle(Request request) {
        NotificationSearchRequest searchRequest = (NotificationSearchRequest) request.getBody();

        NotificationSearchResponse payload = searchRequest.getId() != null && !StringUtils.isEmpty(searchRequest.getGuid())
                ? searchSingleNotificationByDeviceAndId(searchRequest.getId(), searchRequest.getGuid())
                : searchMultipleNotifications(searchRequest);

        return Response.newBuilder()
                .withBody(payload)
                .buildSuccess();
    }

    private NotificationSearchResponse searchMultipleNotifications(NotificationSearchRequest searchRequest) {
        //TODO [rafa] has response is quite bad, instead we should separate command and reply into two separate collections.
        final Collection<DeviceNotification> notifications = storageService.find(
                searchRequest.getGuid(),
                searchRequest.getNames(),
                null,
                0,
                searchRequest.getTimestampStart(),
                searchRequest.getTimestampEnd(),
                null,
                DeviceNotification.class);

        return new NotificationSearchResponse(new ArrayList<>(notifications));
    }

    private NotificationSearchResponse searchSingleNotificationByDeviceAndId(long id, String guid) {
        final List<DeviceNotification> notifications = storageService.find(id, guid, DeviceNotification.class)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
        return new NotificationSearchResponse(notifications);
    }
}

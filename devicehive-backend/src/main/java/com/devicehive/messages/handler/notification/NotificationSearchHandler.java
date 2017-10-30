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

import com.devicehive.model.DeviceNotification;
import com.devicehive.model.rpc.NotificationSearchRequest;
import com.devicehive.model.rpc.NotificationSearchResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.service.helpers.CommandResponseFilterAndSort;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.devicehive.service.helpers.CommandResponseFilterAndSort.buildDeviceNotificationComparator;
import static com.devicehive.service.helpers.CommandResponseFilterAndSort.getTotal;
import static com.devicehive.service.helpers.CommandResponseFilterAndSort.orderAndLimit;

@Component
public class NotificationSearchHandler implements RequestHandler {

    private HazelcastService hazelcastService;

    @Autowired
    public void setHazelcastService(HazelcastService hazelcastService) {
        this.hazelcastService = hazelcastService;
    }

    @Override
    public Response handle(Request request) {
        NotificationSearchRequest searchRequest = (NotificationSearchRequest) request.getBody();

        NotificationSearchResponse payload = searchRequest.getId() != null && !StringUtils.isEmpty(searchRequest.getDeviceId())
                ? searchSingleNotificationByDeviceAndId(searchRequest.getId(), searchRequest.getDeviceId())
                : searchMultipleNotifications(searchRequest);

        return Response.newBuilder()
                .withBody(payload)
                .buildSuccess();
    }

    private NotificationSearchResponse searchMultipleNotifications(NotificationSearchRequest searchRequest) {
        //TODO [rafa] has response is quite bad, instead we should separate command and reply into two separate collections.
        final Collection<DeviceNotification> notifications = hazelcastService.find(
                searchRequest.getDeviceIds(),
                searchRequest.getNames(),
                getTotal(searchRequest.getSkip(), searchRequest.getTake()),
                searchRequest.getTimestampStart(),
                searchRequest.getTimestampEnd(),
                false,
                null,
                DeviceNotification.class);

        final Comparator<DeviceNotification> comparator = buildDeviceNotificationComparator(searchRequest.getSortField());
        String sortOrder = searchRequest.getSortOrder();
        final Boolean reverse = sortOrder == null ? null : "desc".equalsIgnoreCase(sortOrder);

        final List<DeviceNotification> sortedDeviceNotifications = orderAndLimit(new ArrayList<>(notifications),
                comparator, reverse, searchRequest.getSkip(), searchRequest.getTake());

        return new NotificationSearchResponse(new ArrayList<>(sortedDeviceNotifications));
    }

    private NotificationSearchResponse searchSingleNotificationByDeviceAndId(long id, String deviceId) {
        final List<DeviceNotification> notifications = hazelcastService.find(id, deviceId, DeviceNotification.class)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
        return new NotificationSearchResponse(notifications);
    }
}

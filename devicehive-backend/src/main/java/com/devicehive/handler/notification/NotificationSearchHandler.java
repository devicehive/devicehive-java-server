package com.devicehive.handler.notification;

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
        final NotificationSearchResponse notificationSearchResponse = new NotificationSearchResponse();
        final Collection<DeviceNotification> notifications = storageService.find(
                searchRequest.getGuid(),
                searchRequest.getNames(),
                searchRequest.getTimestampStart(),
                searchRequest.getTimestampEnd(),
                DeviceNotification.class);

        notificationSearchResponse.setNotifications(new ArrayList<>(notifications));
        return notificationSearchResponse;
    }

    private NotificationSearchResponse searchSingleNotificationByDeviceAndId(long id, String guid) {
        final NotificationSearchResponse notificationSearchResponse = new NotificationSearchResponse();
        final List<DeviceNotification> notifications = storageService.find(id, guid, DeviceNotification.class)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
        notificationSearchResponse.setNotifications(notifications);
        return notificationSearchResponse;
    }
}

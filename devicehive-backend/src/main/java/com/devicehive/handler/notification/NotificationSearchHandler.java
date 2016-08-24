package com.devicehive.handler.notification;

import com.devicehive.model.DeviceNotification;
import com.devicehive.model.rpc.NotificationSearchRequest;
import com.devicehive.model.rpc.NotificationSearchResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class NotificationSearchHandler implements RequestHandler {

    @Autowired
    private HazelcastService storageService;

    @Override
    public Response handle(Request request) {
        NotificationSearchRequest searchRequest = (NotificationSearchRequest) request.getBody();

        Response serviceResult;

        if (searchRequest.getId() != null) {
            serviceResult = searchSingleNotificationByDeviceAndId(request, searchRequest);
        } else {
            serviceResult = searchMultipleNotifications(request, searchRequest);
        }

        return serviceResult;
    }

    private Response searchMultipleNotifications(Request request, NotificationSearchRequest searchRequest) {
        Response serviceResult;//TODO [rafa] has response is quite bad, instead we should separate command and reply into two separate collections.
        Collection<DeviceNotification> deviceNotifications = storageService.find(searchRequest.getDevices(), searchRequest.getNames(), searchRequest.getTimestamp(),
                searchRequest.getStatus(), searchRequest.getTake(), searchRequest.getHasResponse(), DeviceNotification.class);
        NotificationSearchResponse payload = new NotificationSearchResponse();
        payload.setNotifications(new ArrayList<>());
        payload.getNotifications().addAll(deviceNotifications);
        serviceResult = Response.newBuilder()
                .withBody(payload)
                .withCorrelationId(request.getCorrelationId())
                .withLast(true)
                .buildSuccess();
        return serviceResult;
    }

    private Response searchSingleNotificationByDeviceAndId(Request request, NotificationSearchRequest searchRequest) {
        Response serviceResult;
        Optional<DeviceNotification> deviceNotification = storageService.find(searchRequest.getId(), searchRequest.getGuid(), DeviceNotification.class);

        NotificationSearchResponse payload = new NotificationSearchResponse();
        payload.setNotifications(new ArrayList<>());
        if (deviceNotification != null && deviceNotification.isPresent()) {
            payload.getNotifications().add(deviceNotification.get());
        }
        serviceResult = Response.newBuilder()
                .withBody(payload)
                .withCorrelationId(request.getCorrelationId())
                .withLast(true)
                .buildSuccess();
        return serviceResult;
    }
}

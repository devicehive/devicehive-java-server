package com.devicehive.shim.api.client;

import com.devicehive.shim.api.Response;

import java.util.Collection;
import java.util.Date;
import java.util.function.Consumer;

public interface DeviceNotificationService {

    void find(Long notificationId, String deviceGuid, Consumer<Response> callback);

    void find(Collection<String> deviceGuids, Collection<String> notificationNames, Date fromTimestamp, Integer take, Consumer<Response> callback);

}

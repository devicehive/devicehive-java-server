package com.devicehive.client.impl.rest.subs;

import com.devicehive.client.model.exceptions.HiveException;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;


public interface SubManager {
    void addCommandsSubscription(Map<String, String> headers, Timestamp timestamp,
                                 Set<String> names, String... deviceIds) throws HiveException;

    void addCommandUpdateSubscription(long commandId, String deviceId);

    void removeCommandSubscription(Set<String> names, String... deviceIds) throws HiveException;

    void addNotificationSubscription(Map<String, String> headers, Timestamp timestamp, Set<String> names,
                                     String... deviceIds) throws HiveException;

    void removeNotificationSubscription(Set<String> names, String... deviceIds) throws HiveException;

    void resubscribeAll();
}

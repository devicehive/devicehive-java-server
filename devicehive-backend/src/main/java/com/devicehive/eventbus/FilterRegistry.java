package com.devicehive.eventbus;

/*
 * #%L
 * DeviceHive Backend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.eventbus.Subscriber;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.*;

/**
 * Class for handling all subscriber's filters
 */
public class FilterRegistry {

    /**
     * Table for holding subscription request id (i.e. subscriber) for particular filter.
     * First key is comma-separated string combination of networkId, deviceTypeId and deviceId,
     * second key is comma-separated string combination of eventName and name.
     */
    private final Table<String, String, Set<Subscriber>> subscriberTable = HashBasedTable.create();

    public void register(Filter filter, Subscriber subscriber) {
        Set<Subscriber> subscribers = subscriberTable.get(filter.getFirstKey(), filter.getSecondKey());
        if (subscribers == null) {
            subscribers = new HashSet<>();
            subscribers.add(subscriber);
            subscriberTable.put(filter.getFirstKey(), filter.getSecondKey(), subscribers);
        } else {
            subscribers.add(subscriber);
        }
    }

    public void unregister(Subscriber subscriber) {
        subscriberTable.values().forEach(subscribers -> subscribers.forEach(sub -> {
            if (sub.equals(subscriber)) {
                subscribers.remove(subscriber);
            }
        }));
    }

    Collection<Subscriber> getSubscribers(Filter filter) {
        Set<Subscriber> subscribers = Optional.ofNullable(subscriberTable.get("*,*,*", filter.getSecondKey()))
                .orElse(new HashSet<>());
        Set<Subscriber> filterSubscribers = subscriberTable.get(filter.getDeviceIgnoredFirstKey(), filter.getSecondKey());
        if (filterSubscribers != null) {
            subscribers.addAll(filterSubscribers);
        }
        Set<Subscriber> deviceFilterSubscribers = subscriberTable.get(filter.getFirstKey(), filter.getSecondKey());
        if (deviceFilterSubscribers != null) {
            subscribers.addAll(deviceFilterSubscribers);
        }
        return subscribers;
    }

    public void unregisterDevice(String deviceId) {
        filterSubscriptionsMap.keySet().stream()
                .filter(key ->
                        key.getDeviceIds().contains(deviceId))
                .forEach(key -> {
                    Collection<Long> subscriptionsIds = filterSubscriptionsMap.get(key);
                    subscriptionsIds.forEach(this::unregister);
                    key.deleteDeviceId(deviceId);
                    if (!(key.getDeviceIds().isEmpty() && key.getNetworkIds().isEmpty())) {
                        subscriptionsIds.forEach(id -> register(key, id));
                    }
                });
    }
}

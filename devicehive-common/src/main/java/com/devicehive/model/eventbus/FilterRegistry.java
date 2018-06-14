package com.devicehive.model.eventbus;

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

import com.devicehive.vo.DeviceVO;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.Gson;

import java.util.*;

/**
 * Class for handling all subscriber's filters
 */
public abstract class FilterRegistry {

    /**
     * Table for holding subscription request id (i.e. subscriber) for particular filter.
     * First key is comma-separated string combination of networkId, deviceTypeId and deviceId,
     * second key is comma-separated string combination of eventName and name.
     */
    private final Table<String, String, Set<Subscriber>> subscriberTable = HashBasedTable.create();

    public abstract void register(Filter filter, Subscriber subscriber);

    public abstract void unregister(Subscriber subscriber);

    public abstract void unregisterDevice(DeviceVO device);

    public abstract void unregisterNetwork(Long networkId, Collection<DeviceVO> devices);

    public abstract void unregisterDeviceType(Long deviceTypeId, Collection<DeviceVO> devices);

    protected synchronized void processRegister(Filter filter, Subscriber subscriber) {
        Set<Subscriber> subscribers = subscriberTable.get(filter.getFirstKey(), filter.getSecondKey());
        if (subscribers == null) {
            subscribers = new HashSet<>();
            subscribers.add(subscriber);
            subscriberTable.put(filter.getFirstKey(), filter.getSecondKey(), subscribers);
        } else {
            subscribers.add(subscriber);
        }
    }

    protected synchronized void processUnregister(Subscriber subscriber) {
        subscriberTable.values().forEach(subscribers -> {
            Iterator iterator = subscribers.iterator();

            while (iterator.hasNext()) {
                Object element = iterator.next();
                if (element.equals(subscriber)) {
                    iterator.remove();
                }
            }
        });
    }

    protected synchronized void processUnregisterDevice(DeviceVO device) {
        final Filter deviceFilter = new Filter(device.getNetworkId(), device.getDeviceTypeId(), device.getDeviceId());
        subscriberTable.row(deviceFilter.getFirstKey()).clear();
    }

    protected synchronized void processUnregisterNetwork(Long networkId, Collection<DeviceVO> devices) {
        // Removing subscription on particular network
        final Filter networkFilter = new Filter(networkId, null, null);
        subscriberTable.row(networkFilter.getFirstKey()).clear();
        // Removing subscription on all network's devices
        devices.forEach(this::processUnregisterDevice);
    }

    protected synchronized void processUnregisterDeviceType(Long deviceTypeId, Collection<DeviceVO> devices) {
        // Removing subscription on particular device type
        final Filter deviceTypeFilter = new Filter(null, deviceTypeId, null);
        subscriberTable.row(deviceTypeFilter.getFirstKey()).clear();
        // Removing subscription on all device type's devices
        devices.forEach(this::processUnregisterDevice);
    }

    public Collection<Subscriber> getSubscribers(Filter filter) {
        Set<Subscriber> subscribers = new HashSet<>();
        Set<Subscriber> globalFilterSubscribers = subscriberTable.get("*,*,*", filter.getSecondKey());
        if (globalFilterSubscribers != null) {
            subscribers.addAll(globalFilterSubscribers);
        }
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

    protected void handleSubscriptionMessage(String message, Gson gson) {
        SubscriptionSyncMessage syncMessage = gson.fromJson(message, SubscriptionSyncMessage.class);

        switch (syncMessage.getAction()) {
            case REGISTER:
                processRegister(syncMessage.getFilter(), syncMessage.getSubscriber());
                break;
            case UNREGISTER:
                processUnregister(syncMessage.getSubscriber());
                break;
            case UNREGISTER_DEVICE:
                if (!syncMessage.getDevices().isEmpty()) processUnregisterDevice(syncMessage.getDevices().iterator().next());
                break;
            case UNREGISTER_NETWORK:
                processUnregisterNetwork(syncMessage.getNetworkId(), syncMessage.getDevices());
                break;
            case UNREGISTER_DEVICE_TYPE:
                processUnregisterDeviceType(syncMessage.getDeviceTypeId(), syncMessage.getDevices());
        }
    }
}

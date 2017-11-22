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

import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.eventbus.Filter;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Class for handling all subscriber's filters
 */
public class FilterRegistry {

    private MultiMap<Filter, Long> filterSubscriptionsMap;
    private final String FILTER_SUBSCRIPTION_MAP = "FILTER-SUBSCRIPTION-MAP";

    @Autowired
    public void getHazelcastMaps(HazelcastInstance hazelcastClient) {
        filterSubscriptionsMap = hazelcastClient.getMultiMap(FILTER_SUBSCRIPTION_MAP);
    }

    public void register(Filter filter, Long subscriptionId) {
        HivePrincipal principal = filter.getPrincipal();
        if (filter.isGlobal()) {
            if (!principal.areAllNetworksAvailable()) {
                filter.setGlobal(false);
                filter.setNetworkIds(principal.getNetworkIds());
            }

            if (!principal.areAllDeviceTypesAvailable()) {
                filter.setGlobal(false);
                filter.setNetworkIds(principal.getDeviceTypeIds());
            }
        }
        filterSubscriptionsMap.put(filter, subscriptionId);
    }

    public void unregister(Long subscriptionId) {
        filterSubscriptionsMap.keySet().forEach(filter -> filterSubscriptionsMap.remove(filter, subscriptionId));
    }

    public Filter getFilter(Long subscriptionId) {
        for (Map.Entry<Filter, Long> entry : filterSubscriptionsMap.entrySet()) {
            if (entry.getValue().equals(subscriptionId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Set<Pair<Long, Filter>> getSubscriptions(Long networkId) {
        Set<Pair<Long, Filter>> subs = new HashSet<>();
        filterSubscriptionsMap.keySet().forEach( filter -> {
            if (filter.isGlobal() || (filter.getNetworkIds() != null && filter.getNetworkIds().contains(networkId))) {
                filterSubscriptionsMap.get(filter).forEach(subId -> subs.add(Pair.of(subId, filter)));
            }
        });
        return subs;
    }
}

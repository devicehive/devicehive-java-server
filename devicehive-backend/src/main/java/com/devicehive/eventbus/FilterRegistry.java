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
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Class for handling all subscriber's filters
 */
public class FilterRegistry {

    private final Map<Filter, Set<Long>> filterSubscriptionsMap = new HashMap<>();

    public void register(Filter filter, Long subscriptionId) {
        HivePrincipal principal = filter.getPrincipal();
        if (filter.isGlobal() && principal.areAllDevicesAvailable()) {
            if (!principal.areAllNetworksAvailable()) {
                filter.setGlobal(false);
                filter.setNetworkIds(principal.getNetworkIds());
            }
            addFilter(filter, subscriptionId);
            return;
        }
        Set<Long> networkIds = filter.getNetworkIds();
        if (networkIds != null) {
            addFilter(filter, subscriptionId);
        }
    }

    public void unregister(Long subscriptionId) {
        filterSubscriptionsMap.values().forEach(subscriptionIds -> subscriptionIds.remove(subscriptionId));
    }

    public Set<Pair<Long, Filter>> getSubscriptions(Long networkId) {
        Set<Pair<Long, Filter>> subs = new HashSet<>();
        filterSubscriptionsMap.keySet().forEach( filter -> {
            if (filter.isGlobal() || filter.getNetworkIds().contains(networkId)) {
                filterSubscriptionsMap.get(filter).forEach(subId -> subs.add(Pair.of(subId, filter)));
            }
        });
        return subs;
    }

    private void addFilter(Filter filter, Long subscriptionId) {
        Set<Long> subscriptionIds = filterSubscriptionsMap.get(filter);
        if (subscriptionIds == null) {
            filterSubscriptionsMap.put(filter, Sets.newHashSet(subscriptionId));
        } else {
            subscriptionIds.add(subscriptionId);
        }
    }
}

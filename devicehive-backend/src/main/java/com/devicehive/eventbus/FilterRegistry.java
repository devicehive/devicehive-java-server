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

import java.util.*;

/**
 * Class for handling all subscriber's filters
 */
public class FilterRegistry {

    private final Map<Long, Set<Long>> networkSubscriptions = new HashMap<>();

    private final Set<Long> globalSubscriptions = new HashSet<>();

    private final Map<Long, Filter> subscriptionFilter = new HashMap<>();

    public void register(Filter filter, Long subscriptionId) {
        HivePrincipal principal = filter.getPrincipal();
        if (filter.isGlobal() && principal.areAllDevicesAvailable()) {
            if (principal.areAllNetworksAvailable()) {
                globalSubscriptions.add(subscriptionId);
            } else {
                principal.getNetworkIds().forEach(network -> addNetwork(network, subscriptionId));
            }
            subscriptionFilter.put(subscriptionId, filter);
            return;
        }
        Set<Long> networkIds = filter.getNetworkIds();
        if (networkIds != null) {
            networkIds.forEach(network -> addNetwork(network, subscriptionId));
            subscriptionFilter.put(subscriptionId, filter);
        }
    }

    public void unregister(Long subscriptionId) {
        Filter filter = subscriptionFilter.get(subscriptionId);
        if (filter != null) {
            if (filter.isGlobal()) {
                globalSubscriptions.remove(subscriptionId);
            }
            Set<Long> networkIds = filter.getNetworkIds();
            if (networkIds != null) {
                networkIds.forEach(network -> removeNetwork(network, subscriptionId));
            }
            subscriptionFilter.remove(subscriptionId);
        }
    }

    private Set<Long> getGlobalSubscriptions() {
        return globalSubscriptions;
    }

    private Set<Long> getNetworkSubscriptions(Long networkId) {
        return networkSubscriptions.get(networkId);
    }

    public Set<Long> getSubscriptions(Long networkId) {
        Set<Long> subs = new HashSet<>();
        Set<Long> gSubs = getGlobalSubscriptions();
        if (gSubs != null) {
            subs.addAll(gSubs);
        }
        Set<Long> nSubs = getNetworkSubscriptions(networkId);
        if (nSubs != null) {
            subs.addAll(nSubs);
        }
        return subs;
    }

    public Filter getFilter(Long subscriptionId) {
        return subscriptionFilter.get(subscriptionId);
    }

    private void addNetwork(Long networkId, Long subscriptionId) {
        Set<Long> subscriptionIds = networkSubscriptions.get(networkId);
        if (subscriptionIds == null) {
            networkSubscriptions.put(networkId, Sets.newHashSet(subscriptionId));
        } else {
            subscriptionIds.add(subscriptionId);
        }
    }

    private void removeNetwork(Long networkId, Long subscriptionId) {
        Set<Long> subscriptionIds = networkSubscriptions.get(networkId);
        if (subscriptionIds != null) {
            subscriptionIds.remove(subscriptionId);
            if (subscriptionIds.isEmpty()) {
                networkSubscriptions.remove(networkId);
            }
        }
    }
}

package com.devicehive.websockets;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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


import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.websockets.util.HiveEndpoint;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HiveWebsocketSessionState {

    public static final String KEY = HiveWebsocketSessionState.class.getName();
    private final Lock queueLock = new ReentrantLock(true);
    private final ConcurrentLinkedQueue<JsonElement> queue = new ConcurrentLinkedQueue<>();
    private final Set<UUID> commandSubscriptions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> commandUpdateSubscriptions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Lock commandSubscriptionsLock = new ReentrantLock(true);
    private final Set<UUID> notificationSubscriptions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Lock notificationSubscriptionsLock = new ReentrantLock(true);
    private final Lock commandUpdateSubscriptionsLock = new ReentrantLock(true);
    private final ConcurrentMap<Set<String>, Set<UUID>> oldFormatCommandSubscriptions = Maps.newConcurrentMap();
    private final ConcurrentMap<Set<String>, Set<UUID>> oldFormatNotificationSubscriptions = Maps.newConcurrentMap();
    private HiveEndpoint endpoint;
    private HivePrincipal hivePrincipal;

    public static HiveWebsocketSessionState get(WebSocketSession session) {
        return (HiveWebsocketSessionState) session.getAttributes().get(HiveWebsocketSessionState.KEY);
    }

    public HiveEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(HiveEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Lock getQueueLock() {
        return queueLock;
    }

    public ConcurrentLinkedQueue<JsonElement> getQueue() {
        return queue;
    }

    public Set<UUID> getCommandSubscriptions() {
        return commandSubscriptions;
    }

    public Set<UUID> getCommandUpdateSubscriptions() {
        return commandUpdateSubscriptions;
    }

    public Lock getCommandSubscriptionsLock() {
        return commandSubscriptionsLock;
    }

    public Set<UUID> getNotificationSubscriptions() {
        return notificationSubscriptions;
    }

    public Lock getNotificationSubscriptionsLock() {
        return notificationSubscriptionsLock;
    }

    public HivePrincipal getHivePrincipal() {
        return hivePrincipal;
    }

    public void setHivePrincipal(HivePrincipal hivePrincipal) {
        this.hivePrincipal = hivePrincipal;
    }

    public Lock getCommandUpdateSubscriptionsLock() {
        return commandUpdateSubscriptionsLock;
    }

    public synchronized void addOldFormatCommandSubscription(Set<String> guids, UUID subscriptionId) {
        Set<String> toStore = guids == null
                              ? Sets.newHashSet(Constants.NULL_SUBSTITUTE)
                              : guids;

        if (oldFormatCommandSubscriptions.containsKey(toStore)) {
            Set<UUID> existingSubscriptions = oldFormatCommandSubscriptions.get(toStore);
            existingSubscriptions.add(subscriptionId);
        } else {
            Set<UUID> subscriptions = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
            subscriptions.add(subscriptionId);
            oldFormatCommandSubscriptions.put(toStore, subscriptions);
        }
    }

    public synchronized Set<UUID> removeOldFormatCommandSubscription(Set<String> guids) {
        Set<String> toRemove = guids == null
                               ? new HashSet<String>() {
            {
                add(Constants.NULL_SUBSTITUTE);
            }

            private static final long serialVersionUID = -8106785048967338278L;
        }
                               : guids;
        return oldFormatCommandSubscriptions.remove(toRemove);
    }

    public synchronized void addOldFormatNotificationSubscription(Set<String> guids, UUID subscriptionId) {
        Set<String> toStore = guids == null
                              ? Sets.newHashSet(Constants.NULL_SUBSTITUTE)
                              : guids;

        if (oldFormatNotificationSubscriptions.containsKey(toStore)) {
            Set<UUID> existingSubscriptions = oldFormatNotificationSubscriptions.get(toStore);
            existingSubscriptions.add(subscriptionId);
        } else {
            Set<UUID> subscriptions = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
            subscriptions.add(subscriptionId);
            oldFormatNotificationSubscriptions.put(toStore, subscriptions);
        }
    }

    public synchronized Set<UUID> removeOldFormatNotificationSubscription(Set<String> guids) {
        Set<String> toRemove = guids == null
                               ? new HashSet<String>() {
            {
                add(Constants.NULL_SUBSTITUTE);
            }

            private static final long serialVersionUID = 599925075379032426L;
        }
                               : guids;
        return oldFormatNotificationSubscriptions.remove(toRemove);
    }


}

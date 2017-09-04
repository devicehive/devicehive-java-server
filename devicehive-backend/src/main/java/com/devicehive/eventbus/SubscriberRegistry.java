package com.devicehive.eventbus;

/*
 * #%L
 * DeviceHive Backend Logic
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

import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Class for handling all subscribe, unsubscribe and get subscribers tricky logic
 */
class SubscriberRegistry {

    /**
     * Map for holding subscriptions for particular subscription request id (i.e. subscriber).
     * The KEY in this map is an id of subscriber (subscription request) and the VALUE is a set of subscriptions for this subscriber.
     *
     * This map keeps track of all subscriptions for single subscriber so that it is possible to remove all of them
     * from {@link SubscriberRegistry#subscriptions} map during {@link SubscriberRegistry#unregister(Subscriber)} call
     */
    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<Subscription>> subscriberSubscriptions =
            new ConcurrentHashMap<>();

    /**
     * Map that contains an information about subscribers for single subscription.
     * The KEY is subscription (e.g. subscription on device notifications) and the VALUE is a set of all subscriber's ids.
     * This map is used for actual routing of messages through the event bus
     */
    private final ConcurrentHashMap<Subscription, CopyOnWriteArraySet<Long>> subscriptions =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, Subscriber> subscribers = new ConcurrentHashMap<>();

    /**
     * Registers subscription and subscriber in registry maps.
     * Performs following steps:
     *  - if subscriber doesn't have any subscriptions in {@link SubscriberRegistry#subscriberSubscriptions} - creates an empty list for him;
     *  - adds subscription into subscriber's list in {@link SubscriberRegistry#subscriberSubscriptions};
     *  - if nobody is subscribed to this subscription in {@link SubscriberRegistry#subscriptions} - initializes the list;
     *  - adds subscriber to this subscription's list in {@link SubscriberRegistry#subscriptions}
     *
     * @param subscriber - subscriber
     * @param subscription - subscription to subscribe to
     */
    void register(Subscriber subscriber, Subscription subscription) {
        CopyOnWriteArraySet<Subscription> subscriptions = subscriberSubscriptions.get(subscriber.getId());
        if (subscriptions == null) {
            //initialize list in a thread safe manner
            CopyOnWriteArraySet<Subscription> newSet = new CopyOnWriteArraySet<>();
            subscriptions = firstNonNull(subscriberSubscriptions.putIfAbsent(subscriber.getId(), newSet), newSet);
        }
        subscriptions.add(subscription);

        CopyOnWriteArraySet<Long> subIds = this.subscriptions.get(subscription);
        if (subIds == null) {
            //initialize list in a thread safe manner
            CopyOnWriteArraySet<Long> newSet = new CopyOnWriteArraySet<>();
            subIds = firstNonNull(this.subscriptions.putIfAbsent(subscription, newSet), newSet);
        }
        subIds.add(subscriber.getId());
        subscribers.put(subscriber.getId(), subscriber);
    }

    /**
     * Unregisters subscriber from registry maps:
     *  - gets all subscriber's subscriptions from {@link SubscriberRegistry#subscriberSubscriptions}
     *  - removes subscriber from each subscription's list in {@link SubscriberRegistry#subscriptions}
     *  - removes entry from {@link SubscriberRegistry#subscriberSubscriptions}
     *
     * @param subscriber - subscriber
     */
    void unregister(Subscriber subscriber) {
        CopyOnWriteArraySet<Subscription> subs =
                subscriberSubscriptions.getOrDefault(subscriber.getId(), new CopyOnWriteArraySet<>());
        subs.forEach(s -> {
            CopyOnWriteArraySet<Long> subIds = this.subscriptions.get(s);
            if (subIds != null) {
                Long id = subscriber.getId();
                subIds.remove(id);
                subscribers.remove(id);
            }
        });
    }

    /**
     * @param subscription - subscription
     * @return - list of subscribers for subscription
     */
    Collection<Subscriber> getSubscribers(Subscription subscription) {
        Assert.notNull(subscription);
        return this.subscriptions.getOrDefault(subscription, new CopyOnWriteArraySet<>())
                .stream().map(subscribers::get).collect(Collectors.toList());
    }

    Collection<Subscription> getSubscriptions(Subscriber subscriber) {
        Assert.notNull(subscriber);
        return this.subscriberSubscriptions.getOrDefault(subscriber.getId(), new CopyOnWriteArraySet<>());
    }

    private static <T> T firstNonNull(T first, T second) {
        Assert.notNull(second);
        return first != null ? first : second;
    }

}

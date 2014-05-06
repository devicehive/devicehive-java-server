package com.devicehive.messages.subscriptions;


import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AbstractStorage<E, T extends Subscription<E>> {

    private ConcurrentMap<E, Set<T>> byEventSource = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Set<T>> bySubscriptionId = new ConcurrentHashMap<>();
    private ConcurrentMap<Pair<E, String>, T> byPair = new ConcurrentHashMap<>();

    public synchronized void insertAll(Collection<T> coll) {
        for (T t : coll) {
            insert(t);
        }
    }

    public synchronized boolean insert(T subscription) {
        if (byPair.containsKey(ImmutablePair.of(subscription.getEventSource(), subscription.getSubscriptionId()))) {
            return false;
        }
        Set<T> set = byEventSource.get(subscription.getEventSource());
        if (set == null) {
            set = Collections.newSetFromMap(new ConcurrentHashMap<T, Boolean>());
            byEventSource.put(subscription.getEventSource(), set);
        }
        set.add(subscription);

        set = bySubscriptionId.get(subscription.getSubscriptionId());
        if (set == null) {
            set = Collections.newSetFromMap(new ConcurrentHashMap<T, Boolean>());
            bySubscriptionId.put(subscription.getSubscriptionId(), set);
        }
        set.add(subscription);

        byPair.put(ImmutablePair.of(subscription.getEventSource(), subscription.getSubscriptionId()), subscription);
        return true;
    }

    public Set<T> get(E eventPoint) {
        Set<T> set = byEventSource.get(eventPoint);
        return set != null ? set : Collections.EMPTY_SET;
    }

    public Set<T> get(String id) {
        Set<T> set = bySubscriptionId.get(id);
        return set != null ? set : Collections.EMPTY_SET;
    }

    public synchronized void remove(T subscription) {
        remove(subscription.getEventSource(), subscription.getSubscriptionId());
    }

    public synchronized void removeAll(Collection<T> coll) {
        for (T t : coll) {
            remove(t);
        }
    }

    public synchronized void removePairs(Collection<Pair<E, String>> coll) {
        for (Pair<E, String> pair : coll) {
            remove(pair.getKey(), pair.getValue());
        }
    }

    public synchronized void remove(E eventSource, String subscriberId) {
        T sub = byPair.remove(ImmutablePair.of(eventSource, subscriberId));
        if (sub == null) {
            return;
        }

        Set<T> subs = byEventSource.get(sub.getEventSource());
        subs.remove(sub);
        if (subs.isEmpty()) {
            byEventSource.remove(sub.getEventSource());
        }

        subs = bySubscriptionId.get(sub.getSubscriptionId());
        subs.remove(sub);
        if (subs.isEmpty()) {
            bySubscriptionId.remove(sub.getSubscriptionId());
        }
    }

    protected synchronized void removeByEventSource(E eventSource) {
        Set<T> subs = byEventSource.remove(eventSource);
        if (subs == null) {
            return;
        }
        for (T sub : new ArrayList<T>(subs)) {
            remove(sub.getEventSource(), sub.getSubscriptionId());
        }
    }

    public synchronized void removeBySubscriptionId(String id) {
        Set<T> subs = bySubscriptionId.get(id);
        if (subs == null) {
            return;
        }
        for (T sub : new ArrayList<T>(subs)) {
            remove(sub.getEventSource(), sub.getSubscriptionId());
        }
    }
}

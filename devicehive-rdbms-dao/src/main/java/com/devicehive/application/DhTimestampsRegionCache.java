package com.devicehive.application;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.hibernate.local.Timestamp;
import com.hazelcast.hibernate.local.TimestampsRegionCache;
import com.hazelcast.hibernate.serialization.Expirable;
import com.hazelcast.hibernate.serialization.Value;

public class DhTimestampsRegionCache extends TimestampsRegionCache {

    public DhTimestampsRegionCache(String name, HazelcastInstance hazelcastInstance) {
        super(name, hazelcastInstance);
    }

    @Override
    protected void maybeInvalidate(final Object messageObject) {
        Timestamp ts = (Timestamp) messageObject;
        final Object key = ts.getKey();

        if (ts.getTimestamp() > System.currentTimeMillis()) {
            ts = new Timestamp(ts.getKey(), System.currentTimeMillis());
        }

        for (; ; ) {
            final Expirable value = cache.get(key);
            final Long current = value != null ? (Long) value.getValue() : null;
            if (current != null) {
                if (ts.getTimestamp() > current) {
                    if (cache.replace(key, value, new Value(value.getVersion(), nextTimestamp(), ts.getTimestamp()))) {
                        return;
                    }
                } else {
                    return;
                }
            } else {
                if (cache.putIfAbsent(key, new Value(null, nextTimestamp(), ts.getTimestamp())) == null) {
                    return;
                }
            }
        }
    }
}

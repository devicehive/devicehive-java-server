package com.devicehive.application;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.hibernate.local.Timestamp;
import com.hazelcast.hibernate.local.TimestampsRegionCache;
import com.hazelcast.hibernate.serialization.Expirable;
import com.hazelcast.hibernate.serialization.Value;

// Fix for issues described in https://github.com/hazelcast/hazelcast/issues/13271 and https://github.com/hazelcast/hazelcast-hibernate5/issues/33
// Monitor when either of those are fixed, after that we should remove this and DhLocalCacheRegionFactory
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

        while (true) {
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

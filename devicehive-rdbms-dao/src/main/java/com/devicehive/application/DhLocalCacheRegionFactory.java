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

import com.hazelcast.hibernate.HazelcastLocalCacheRegionFactory;
import com.hazelcast.hibernate.local.LocalRegionCache;
import com.hazelcast.hibernate.region.HazelcastTimestampsRegion;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.TimestampsRegion;

import java.util.Properties;

// Fix for issues described in https://github.com/hazelcast/hazelcast/issues/13271 and https://github.com/hazelcast/hazelcast-hibernate5/issues/33
// Monitor when either of those are fixed, after that we should remove this and DhTimestampsRegionaCache
public class DhLocalCacheRegionFactory extends HazelcastLocalCacheRegionFactory {

    @Override
    public TimestampsRegion buildTimestampsRegion(final String regionName, final Properties properties)
            throws CacheException {
        return new HazelcastTimestampsRegion<LocalRegionCache>(instance, regionName, properties,
                new DhTimestampsRegionCache(regionName, instance));
    }
}

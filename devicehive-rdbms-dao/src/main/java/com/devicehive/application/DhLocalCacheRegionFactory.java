package com.devicehive.application;

import com.hazelcast.hibernate.HazelcastLocalCacheRegionFactory;
import com.hazelcast.hibernate.local.LocalRegionCache;
import com.hazelcast.hibernate.region.HazelcastTimestampsRegion;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.TimestampsRegion;

import java.util.Properties;

public class DhLocalCacheRegionFactory extends HazelcastLocalCacheRegionFactory {

    @Override
    public TimestampsRegion buildTimestampsRegion(final String regionName, final Properties properties)
            throws CacheException {
        return new HazelcastTimestampsRegion<LocalRegionCache>(instance, regionName, properties,
                new DhTimestampsRegionCache(regionName, instance));
    }
}

package com.devicehive.application.hazelcast;

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

import com.devicehive.util.ApplicationContextHolder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.hibernate.HazelcastCacheRegionFactory;
import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Settings;

import java.util.Properties;

public class HazelcastCustomCacheRegionFactory extends HazelcastCacheRegionFactory {

    public HazelcastCustomCacheRegionFactory() {
        this.instance = ApplicationContextHolder.getApplicationContext().getBean(HazelcastInstance.class);
    }

    @Override
    public void start(Settings settings, Properties properties) throws CacheException {
        super.start(settings, properties);
    }
}

package com.devicehive.service;

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

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.HazelcastEntity;
import com.devicehive.model.HazelcastEntityComparator;
import com.devicehive.service.helpers.HazelcastHelper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.PagingPredicate;
import com.hazelcast.query.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

import static com.devicehive.model.enums.SearchableField.LAST_UPDATED;
import static com.devicehive.model.enums.SearchableField.TIMESTAMP;

@Service
public class HazelcastService {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastService.class);

    private static final String NOTIFICATIONS_MAP = "NOTIFICATIONS-MAP";
    private static final String COMMANDS_MAP = "COMMANDS-MAP";

    private final HazelcastInstance hazelcastClient;

    private final HazelcastHelper hazelcastHelper;

    private Map<Class, IMap<String, HazelcastEntity>> mapsHolder = new HashMap<>(2);

    @Autowired
    public HazelcastService(HazelcastInstance hazelcastClient, HazelcastHelper hazelcastHelper) {
        this.hazelcastClient = hazelcastClient;
        this.hazelcastHelper = hazelcastHelper;
    }

    @PostConstruct
    protected void init() {
        final IMap<String, HazelcastEntity> notificationsMap = hazelcastClient.getMap(NOTIFICATIONS_MAP);
        notificationsMap.addIndex(TIMESTAMP.getField(), true);

        final IMap<String, HazelcastEntity> commandsMap = hazelcastClient.getMap(COMMANDS_MAP);
        commandsMap.addIndex(TIMESTAMP.getField(), true);
        commandsMap.addIndex(LAST_UPDATED.getField(), true);
        
        mapsHolder.put(DeviceNotification.class, notificationsMap);
        mapsHolder.put(DeviceCommand.class, commandsMap);
    }


    public <T extends HazelcastEntity> Optional<T> find(Long id, String deviceId, Class<T> entityClass) {
        final Predicate filters = hazelcastHelper.prepareFilters(id, deviceId, entityClass);
        return find(filters, 1, entityClass).stream().findFirst();
    }

    public <T extends HazelcastEntity> Optional<T> find(Long id, String deviceId, boolean returnUpdated, Class<T> entityClass) {
        final Predicate filters = hazelcastHelper.prepareFilters(id, deviceId, returnUpdated, entityClass);
        return find(filters, 1, entityClass).stream().findFirst();
    }

    public <T extends HazelcastEntity> Collection<T> find(Collection<String> deviceIds,
                                                          Collection<String> names,
                                                          Integer take,
                                                          Date timestampSt,
                                                          Date timestampEnd,
                                                          boolean returnUpdated,
                                                          String status,
                                                          Class<T> entityClass) {
        final Predicate filters = hazelcastHelper.prepareFilters(deviceIds,  names, timestampSt, timestampEnd,
               returnUpdated, status, entityClass);
        return find(filters, take, entityClass);
    }

    public <T extends HazelcastEntity> Collection<T> find(String deviceId,
                                                          Collection<Long> networkIds,
                                                          Collection<Long> deviceTypeIds,
                                                          Collection<String> names,
                                                          Integer take,
                                                          Date timestampSt,
                                                          Date timestampEnd,
                                                          boolean returnUpdated,
                                                          String status,
                                                          Class<T> entityClass) {
        final Predicate filters = hazelcastHelper.prepareFilters(deviceId, networkIds, deviceTypeIds, names,
                timestampSt, timestampEnd, returnUpdated, status, entityClass);
        return find(filters, take, entityClass);
    }

    public <T extends HazelcastEntity> void store(final T hzEntity) {
        logger.debug("Saving entity into hazelcast. [Entity: {}]", hzEntity);
        mapsHolder.get(hzEntity.getClass()).set(hzEntity.getHazelcastKey(), hzEntity);
    }

    @SuppressWarnings("unchecked")
    private <T extends HazelcastEntity> Collection<T> find(Predicate predicate, int pageSize, Class<T> tClass) {
        final Predicate pagingPredicate = (pageSize > 0)
                ? new PagingPredicate(predicate, new HazelcastEntityComparator(), pageSize)
                : predicate;
        return (Collection<T>) mapsHolder.get(tClass).values(pagingPredicate);
    }
}

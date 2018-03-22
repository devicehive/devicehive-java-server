package com.devicehive.service.helpers;

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
import com.devicehive.model.HazelcastEntity;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static com.devicehive.model.enums.SearchableField.*;

@Component
public class HazelcastHelper {

    public <T extends HazelcastEntity> Predicate prepareFilters(final Long id, final String deviceId, Class<T> entityClass) {
        return prepareFilters(id, Collections.singleton(deviceId), null, null, null, null, null, false, null, entityClass);
    }

    public <T extends HazelcastEntity> Predicate prepareFilters(final Long id, final String deviceId, final boolean returnUpdated, Class<T> entityClass) {
        return prepareFilters(id, Collections.singleton(deviceId), null, null, null, null, null, returnUpdated, null, entityClass);
    }

    public <T extends HazelcastEntity> Predicate prepareFilters(Collection<String> deviceIds, Collection<String> names,
            Date timestampSt, Date timestampEnd, boolean returnUpdated, String status, Class<T> entityClass) {
        return prepareFilters(null, deviceIds, null, null, names, timestampSt, timestampEnd, returnUpdated, status, entityClass);
    }

    public <T extends HazelcastEntity> Predicate prepareFilters(String deviceId, Collection<Long> networkIds,
            Collection<Long> deviceTypeIds, Collection<String> names, Date timestampSt, Date timestampEnd,
            boolean returnUpdated, String status, Class<T> entityClass) {
        Set<String> deviceIdSet = deviceId != null ? Collections.singleton(deviceId) : null;
        return prepareFilters(null, deviceIdSet, networkIds, deviceTypeIds, names, timestampSt, timestampEnd, returnUpdated, status, entityClass);
    }

    private <T extends HazelcastEntity> Predicate prepareFilters(Long id, Collection<String> deviceIds, Collection<Long> networkIds,
            Collection<Long> deviceTypeIds, Collection<String> names, Date timestampSt, Date timestampEnd,
            boolean returnUpdated, String status, Class<T> entityClass) {
        final List<Predicate> predicates = new ArrayList<>();
        if (id != null) {
            predicates.add(Predicates.equal(ID.getField(), id));
        }

        if (deviceIds != null && !deviceIds.isEmpty()) {
            predicates.add(Predicates.in(DEVICE_IDS.getField(), deviceIds.toArray(new String[deviceIds.size()])));
        }

        if (networkIds != null && !networkIds.isEmpty()) {
            predicates.add(Predicates.in(NETWORK_IDS.getField(), networkIds.toArray(new Long[networkIds.size()])));
        }

        if (deviceTypeIds != null && !deviceTypeIds.isEmpty()) {
            predicates.add(Predicates.in(DEVICE_TYPE_IDS.getField(), deviceTypeIds.toArray(new Long[deviceTypeIds.size()])));
        }

        String searchableField = entityClass.equals(DeviceCommand.class) ? COMMAND.getField() : NOTIFICATION.getField();
        
        if (!CollectionUtils.isEmpty(names)) {
            predicates.add(Predicates.in(searchableField, names.toArray(new String[names.size()])));
        }
        
        if (returnUpdated) {
            predicates.add(Predicates.equal(IS_UPDATED.getField(), returnUpdated));
        }

        if (timestampSt != null) {
            String searchableFieldSt = returnUpdated ? LAST_UPDATED.getField() : TIMESTAMP.getField();
            predicates.add(Predicates.greaterThan(searchableFieldSt, timestampSt.getTime()));
        }

        if (timestampEnd != null) {
            String searchableFieldEnd = returnUpdated ? LAST_UPDATED.getField() : TIMESTAMP.getField();
            predicates.add(Predicates.lessThan(searchableFieldEnd, timestampEnd.getTime()));
        }


        if (StringUtils.isNotEmpty(status)) {
            predicates.add(Predicates.equal(STATUS.getField(), status));
        }

        final Predicate[] predicatesArray = new Predicate[predicates.size()];
        for (int i = 0; i < predicates.size(); i++) {
            predicatesArray[i] = predicates.get(i);
        }

        return Predicates.and(predicatesArray);
    }
}

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

import com.devicehive.entity.HazelcastEntity;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.devicehive.model.enums.SearchableField.*;

@Component
public class HazelcastHelper {

    public Predicate prepareFilters(final Long id, final String deviceId) {
        return prepareFilters(id, deviceId, null, null, null, null, null, null);
    }

    public <T extends HazelcastEntity> Predicate prepareFilters(final String deviceId,
                                                                final Collection<String> names,
                                                                final Collection<String> devices,
                                                                final Date timestampSt, final Date timestampEnd,
                                                                final String status, Class<T> entityClass) {
        if (entityClass.equals(DeviceCommand.class)) {
            return prepareFilters(null, deviceId, devices, null, names, timestampSt, timestampEnd, status);
        }
        if (entityClass.equals(DeviceNotification.class)) {
            return prepareFilters(null, deviceId, devices, names, null, timestampSt, timestampEnd, status);
        }
        return null;
    }

    private Predicate prepareFilters(Long id, String deviceId, Collection<String> devices, Collection<String> notifications,
                                     Collection<String> commands, Date timestampSt, Date timestampEnd,
                                     String status) {
        final List<Predicate> predicates = new ArrayList<>();
        if (id != null) {
            predicates.add(Predicates.equal(ID.getField(), id));
        }

        if (StringUtils.isNotEmpty(deviceId)) {
            predicates.add(Predicates.equal(DEVICE_ID.getField(), deviceId));
        }

        if (devices != null && !devices.isEmpty()) {
            predicates.add(Predicates.in(DEVICE_IDS.getField(), devices.toArray(new String[devices.size()])));
        }

        if (notifications != null && !notifications.isEmpty()) {
            predicates.add(Predicates.in(NOTIFICATION.getField(), notifications.toArray(new String[notifications.size()])));
        } else if (commands != null && !commands.isEmpty()) {
            predicates.add(Predicates.in(COMMAND.getField(), commands.toArray(new String[commands.size()])));
        }

        if (timestampSt != null) {
            predicates.add(Predicates.greaterThan(TIMESTAMP.getField(), timestampSt.getTime()));
        }

        if (timestampEnd != null) {
            predicates.add(Predicates.lessThan(TIMESTAMP.getField(), timestampEnd.getTime()));
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

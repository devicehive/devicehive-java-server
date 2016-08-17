package com.devicehive.service.helpers;

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

    public Predicate prepareFilters(final Long id, final String guid) {
        return prepareFilters(id, guid, null, null, null, null, null, null);
    }

    public Predicate prepareFilters(final Collection<String> devices,
                                                              final Collection<String> commands,
                                                              final Date timestamp, final String status,
                                                              final Boolean hasResponse) {
        return prepareFilters(null, null, devices, null, commands, timestamp, status, hasResponse);
    }

    public Predicate prepareFilters(final Long id, final String guid,
                                                              final Collection<String> devices,
                                                              final Collection<String> notifications,
                                                              final Date timestamp) {
        return prepareFilters(id, guid, devices, notifications, null, timestamp, null, null);
    }

    public Predicate prepareFilters(Long id, String guid, Collection<String> devices, Collection<String> notifications,
                                    Collection<String> commands, Date timestamp, String status, Boolean hasResponse) {
        final List<Predicate> predicates = new ArrayList<>();
        if (id != null) {
            predicates.add(Predicates.equal(ID.getField(), id));
        }

        if (StringUtils.isNotEmpty(guid)) {
            predicates.add(Predicates.equal(GUID.getField(),guid));
        }

        if (devices != null && !devices.isEmpty()) {
            predicates.add(Predicates.in(DEVICE_GUID.getField(), devices.toArray(new String[devices.size()])));
        }

        if (notifications != null && !notifications.isEmpty()) {
            predicates.add(Predicates.in(NOTIFICATION.getField(), notifications.toArray(new String[notifications.size()])));
        }

        if (commands != null && !commands.isEmpty()) {
            predicates.add(Predicates.in(COMMAND.getField(), commands.toArray(new String[commands.size()])));
        }

        if (timestamp != null) {
            predicates.add(Predicates.greaterThan(TIMESTAMP.getField(), timestamp));
        }

        if (StringUtils.isNotEmpty(status)) {
            predicates.add(Predicates.equal(STATUS.getField(), status));
        }

        if (hasResponse != null) {
            predicates.add(Predicates.equal(IS_UPDATED.getField(), hasResponse));
        }

        final Predicate [] predicatesArray = new Predicate[predicates.size()];
        for (int i = 0; i < predicates.size(); i++) {
            predicatesArray[i] = predicates.get(i);
        }

        return Predicates.and(predicatesArray);
    }
}
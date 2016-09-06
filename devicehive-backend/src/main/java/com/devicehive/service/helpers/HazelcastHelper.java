package com.devicehive.service.helpers;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.HazelcastEntity;
import com.devicehive.model.updates.DeviceClassUpdate;
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

    public <T extends HazelcastEntity> Predicate prepareFilters(final String guid,
                                                                final Collection<String> names,
                                                                final Collection<String> devices,
                                                                final Date timestampSt, final Date timestampEnd,
                                                                final String status, Class<T> entityClass) {
        if (entityClass.equals(DeviceCommand.class)) {
            return prepareFilters(null, guid, devices, null, names, timestampSt, timestampEnd, status);
        }
        if (entityClass.equals(DeviceNotification.class)) {
            return prepareFilters(null, guid, devices, names, null, timestampSt, timestampEnd, status);
        }
        return null;
    }

    private Predicate prepareFilters(Long id, String guid, Collection<String> devices, Collection<String> notifications,
                                     Collection<String> commands, Date timestampSt, Date timestampEnd,
                                     String status) {
        final List<Predicate> predicates = new ArrayList<>();
        if (id != null) {
            predicates.add(Predicates.equal(ID.getField(), id));
        }

        if (StringUtils.isNotEmpty(guid)) {
            predicates.add(Predicates.equal(GUID.getField(), guid));
        }

        if (devices != null && !devices.isEmpty()) {
            predicates.add(Predicates.in(DEVICE_GUID.getField(), devices.toArray(new String[devices.size()])));
        }

        if (notifications != null && !notifications.isEmpty()) {
            predicates.add(Predicates.in(NOTIFICATION.getField(), notifications.toArray(new String[notifications.size()])));
        } else if (commands != null && !commands.isEmpty()) {
            predicates.add(Predicates.in(COMMAND.getField(), commands.toArray(new String[commands.size()])));
        }

        if (timestampSt != null) {
            predicates.add(Predicates.greaterThan(TIMESTAMP.getField(), timestampSt));
        }

        if (timestampEnd != null) {
            predicates.add(Predicates.lessThan(TIMESTAMP.getField(), timestampEnd));
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
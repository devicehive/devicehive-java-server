package com.devicehive.service.helpers;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.service.DeviceService;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.devicehive.model.enums.SearchableField.*;

@Component
public class HazelcastHelper {

    @Autowired
    private DeviceService deviceService;

    public Predicate prepareFilters(final Long id, final String guid) {
        return prepareFilters(id, guid, null, null, null, null, null, null, null);
    }

    public Predicate prepareFilters(final Collection<String> devices,
                                                              final Collection<String> commands,
                                                              final Date timestamp, final String status,
                                                              final Boolean isUpdated,
                                                              final HivePrincipal principal) {
        return prepareFilters(null, null, devices, null, commands, timestamp, status, isUpdated, principal);
    }

    public Predicate prepareFilters(final Long id, final String guid,
                                                              final Collection<String> devices,
                                                              final Collection<String> notifications,
                                                              final Date timestamp,
                                                              final HivePrincipal principal) {
        return prepareFilters(id, guid, devices, notifications, null, timestamp, null, null, principal);
    }

    public Predicate prepareFilters(Long id, String guid, Collection<String> devices, Collection<String> notifications,
                                    Collection<String> commands, Date timestamp, String status, Boolean hasResponse,
                                    HivePrincipal principal) {
        final List<Predicate> predicates = new ArrayList<>();
        if (id != null) {
            predicates.add(Predicates.equal(ID.getField(), id));
        }

        if (StringUtils.isNotEmpty(guid)) {
            predicates.add(Predicates.equal(GUID.getField(),guid));
        }

        if (devices != null && !devices.isEmpty() && principal != null) {
            final List<String> availableDevices = deviceService.findGuidsWithPermissionsCheck(devices, principal);
            predicates.add(Predicates.in(DEVICE_GUID.getField(), availableDevices.toArray(new String[availableDevices.size()])));
        }

        if (notifications != null && !notifications.isEmpty()) {
            predicates.add(Predicates.in(NOTIFICATION.getField(), notifications.toArray(new String[notifications.size()])));
        }

        if (commands != null && !commands.isEmpty()) {
            predicates.add(Predicates.in(COMMAND.getField(), commands.toArray(new String[commands.size()])));
        }

        if (timestamp != null) {
            predicates.add(Predicates.greaterEqual(TIMESTAMP.getField(), timestamp));
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
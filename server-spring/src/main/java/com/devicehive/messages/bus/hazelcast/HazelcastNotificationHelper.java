package com.devicehive.messages.bus.hazelcast;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.DeviceNotification;
import com.devicehive.service.DeviceService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.PagingPredicate;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.util.*;

@Repository
public class HazelcastNotificationHelper {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastNotificationHelper.class);

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private DeviceService deviceService;

    private IMap<String, DeviceNotification> notificationsMap;

    @PostConstruct
    protected void init() {
        notificationsMap = hazelcastInstance.getMap("NOTIFICATIONS-MAP");
        notificationsMap.addIndex("id", true);
        notificationsMap.addIndex("deviceGuid", true);
        notificationsMap.addIndex("timestamp", true);
        notificationsMap.addIndex("notification", true);
    }

    public void store(final DeviceNotification deviceNotification) {
        notificationsMap.put(deviceNotification.getHazelcastKey(), deviceNotification);
    }

    public Collection<DeviceNotification> retrieve(Map<String, Comparable []> filters, int pageSize) {
        final List<Predicate> predicates = new ArrayList<>(filters.size());
        if (filters.containsKey("id")) {
            predicates.add(Predicates.equal("id", filters.get("id")[0]));
        }

        if (filters.containsKey("guid")) {
            predicates.add(Predicates.equal("deviceGuid", filters.get("guid")[0]));
        }

        if (filters.containsKey("timestamp")) {
            predicates.add(Predicates.greaterEqual("timestamp", filters.get("timestamp")[0]));
        }

        if (filters.containsKey("deviceGuid")) {
            predicates.add(Predicates.in("deviceGuid", filters.get("deviceGuid")));
        }

        if (filters.containsKey("notification")) {
            predicates.add(Predicates.in("notification", filters.get("notification")));
        }

        //Could not convert list of predicated to array
        final Predicate [] predicatesArray = new Predicate[predicates.size()];
        for (int i = 0; i < predicates.size(); i++) {
            predicatesArray[i] = predicates.get(i);
        }

        final Predicate predicate = Predicates.and(predicatesArray);
        if (pageSize <= 0) {
            return notificationsMap.values(predicate);
        } else {
            final PagingPredicate pagingPredicate = new PagingPredicate(predicate, new DeviceNotificationComparator(), pageSize);
            return notificationsMap.values(pagingPredicate);
        }
    }

    public Map<String, Comparable []> prepareFilters(final Long id,
                                                     final String guid,
                                                     final Collection<String> devices,
                                                     final Collection<String> names,
                                                     final Timestamp timestamp,
                                                     final HivePrincipal principal) {
        final Map<String, Comparable []> filters = new HashMap<>();
        if (id != null) {
            final Long [] idArray = {id};
            filters.put("id", idArray);
        }

        if (guid != null && !guid.isEmpty()) {
            final String [] guidArray = {guid};
            filters.put("guid", guidArray);
        }

        if (devices != null && !devices.isEmpty() && principal != null) {
            final List<String> availableDevices = deviceService.findGuidsWithPermissionsCheck(devices, principal);
            filters.put("deviceGuid", availableDevices.toArray(new String[availableDevices.size()]));
        }

        if (names != null && !names.isEmpty()) {
            filters.put("notification", names.toArray(new String[names.size()]));
        }

        if (timestamp != null) {
            final Timestamp [] array = {timestamp};
            filters.put("timestamp", array);
        }

        return filters;
    }

    private class DeviceNotificationComparator implements Comparator<Map.Entry> {
        @Override
        public int compare(Map.Entry o1, Map.Entry o2) {
            final Timestamp o1Time = ((DeviceNotification) o1.getValue()).getTimestamp();
            final Timestamp o2Time = ((DeviceNotification) o2.getValue()).getTimestamp();

            return o2Time.compareTo(o1Time);
        }
    }
}

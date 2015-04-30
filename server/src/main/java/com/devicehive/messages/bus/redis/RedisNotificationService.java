package com.devicehive.messages.bus.redis;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.util.LogExecutionTime;
import org.apache.commons.collections.CollectionUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.sql.Timestamp;
import java.util.*;

/**
 * Created by tmatvienko on 4/15/15.
 */
@Stateless
@LogExecutionTime
public class RedisNotificationService {
    private static final String KEY_FORMAT = "notification:%s:%s:%s";

    @EJB
    private RedisConnector redis;
    @EJB
    private PropertiesService propertiesService;

    public void save(DeviceNotification deviceNotification) {
        final String key = String.format(KEY_FORMAT, deviceNotification.getDeviceGuid(), deviceNotification.getId(), deviceNotification.getTimestamp());
        Map<String, String> notificationMap = new HashMap<>();
        notificationMap.put("id", deviceNotification.getId().toString());
        notificationMap.put("deviceGuid", deviceNotification.getDeviceGuid());
        notificationMap.put("notification", deviceNotification.getNotification());
        if (deviceNotification.getParameters() != null) {
            notificationMap.put("parameters", deviceNotification.getParameters().getJsonString());
        }
        notificationMap.put("timestamp", TimestampAdapter.formatTimestamp(deviceNotification.getTimestamp()));
        redis.setAll(key, notificationMap, propertiesService.getProperty(Constants.NOTIFICATION_EXPIRE_SEC));
    }

    public DeviceNotification get(final String key, final boolean filterByDate, final boolean filterByName, final Timestamp timestamp, final Collection<String> names) {
        Map<String, String> notificationMap = redis.getAll(key);
        if (!notificationMap.isEmpty()) {
            final Timestamp notificationTimestamp = TimestampAdapter.parseTimestamp(notificationMap.get("timestamp"));
            final boolean skip = (filterByDate && notificationTimestamp.before(timestamp) || (filterByName && !names.contains(notificationMap.get("notification"))));
            if (!skip) {
                DeviceNotification notification = new DeviceNotification();
                notification.setId(Long.valueOf(notificationMap.get("id")));
                notification.setDeviceGuid(notificationMap.get("deviceGuid"));
                notification.setNotification(notificationMap.get("notification"));
                if (notificationMap.get("parameters") != null) {
                    notification.setParameters(new JsonStringWrapper(notificationMap.get("parameters")));
                }
                notification.setTimestamp(notificationTimestamp);
                return notification;
            }
        }
        return null;
    }

    public DeviceNotification getByIdAndGuid(final Long id, final String guid) {
        final Set<String> keys = getAllKeysByIdAndGuid(id, guid);
        if (CollectionUtils.isNotEmpty(keys)) {
            TreeSet<DeviceNotification> notifications = new TreeSet<DeviceNotification>(new DeviceNotificationComparator());
            for (final String key : keys) {
                final DeviceNotification notification = get(key, false, false, null, null);
                if (notification != null) {
                    notifications.add(notification);
                }
            }
            return !notifications.isEmpty() ? notifications.first() : null;
        }
        return null;
    }

    public Collection<DeviceNotification> getByGuids(final Collection<String> guids, final Timestamp timestamp, final Collection<String> names) {
        final Set<String> keys = getAllKeysByGuids(guids);
        if (CollectionUtils.isNotEmpty(keys)) {
            TreeSet<DeviceNotification> notifications = new TreeSet<DeviceNotification>(new DeviceNotificationComparator());
            final boolean filterByDate = timestamp != null;
            final boolean filterByName = CollectionUtils.isNotEmpty(names);
            for (final String key : keys) {
                final DeviceNotification notification = get(key, filterByDate, filterByName, timestamp, names);
                if (notification != null) {
                    notifications.add(notification);
                }
            }
            return notifications;
        }
        return Collections.emptyList();
    }

    public Collection<DeviceNotification> getAll(final Timestamp timestamp, final Collection<String> names) {
        Set<String> keys = redis.getAllKeys(String.format(KEY_FORMAT, "*", "*", "*"));
        if (CollectionUtils.isNotEmpty(keys)) {
            Set<DeviceNotification> notifications = new TreeSet<DeviceNotification>(new DeviceNotificationComparator());
            final boolean filterByDate = timestamp != null;
            final boolean filterByName = CollectionUtils.isNotEmpty(names);
            for (final String key : keys) {
                final DeviceNotification notification = get(key, filterByDate, filterByName, timestamp, names);
                if (notification != null) {
                    notifications.add(notification);
                }
            }
            return notifications;
        }
        return Collections.emptyList();
    }

    private Set<String> getAllKeysByIdAndGuid(final Long id, final String guid) {
        return redis.getAllKeys(String.format(KEY_FORMAT, guid, id, "*"));
    }

    private Set<String> getAllKeysByGuids(final Collection<String> guids) {
        if (CollectionUtils.isNotEmpty(guids)) {
            Set<String> keys = new HashSet<>();
            for (String guid : guids) {
                keys.addAll(redis.getAllKeys(String.format(KEY_FORMAT, guid, "*", "*")));
            }
            return keys;
        }
        return Collections.emptySet();
    }

    private class DeviceNotificationComparator implements Comparator<DeviceNotification> {

        @Override
        public int compare(DeviceNotification o1, DeviceNotification o2) {
            return o2.getTimestamp().compareTo(o1.getTimestamp());
        }
    }
}

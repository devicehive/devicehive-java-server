package com.devicehive.messages.bus.redis;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.util.LogExecutionTime;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.*;

/**
 * Created by tmatvienko on 4/15/15.
 */
@Stateless
@LogExecutionTime
public class RedisNotificationService extends RedisService<DeviceNotification> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisNotificationService.class);
    private static final String KEY_FORMAT = "notification:%s:%s";

    @EJB
    private RedisConnector redis;
    @EJB
    private PropertiesService propertiesService;

    @Override
    @Asynchronous
    public void save(DeviceNotification deviceNotification) {
        final String key = String.format(KEY_FORMAT, deviceNotification.getDeviceGuid(), deviceNotification.getId());
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

    @Override
    public DeviceNotification getByKey(String key) {
        Map<String, String> notificationMap = redis.getAll(key);
        if (!notificationMap.isEmpty()) {
            DeviceNotification notification = new DeviceNotification();
            notification.setId(Long.valueOf(notificationMap.get("id")));
            notification.setDeviceGuid(notificationMap.get("deviceGuid"));
            notification.setNotification(notificationMap.get("notification"));
            if (notificationMap.get("parameters") != null) {
                notification.setParameters(new JsonStringWrapper(notificationMap.get("parameters")));
            }
            notification.setTimestamp(TimestampAdapter.parseTimestamp(notificationMap.get("timestamp")));
            return notification;
        }
        return null;
    }

    @Override
    public DeviceNotification getByIdAndGuid(Long id, String guid) {
        final String key = String.format(KEY_FORMAT, guid, id);
        return getByKey(key);
    }

    @Override
    public List<DeviceNotification> getByGuids(Collection<String> guids) {
        final List<String> keys = getAllKeysByGuids(guids);
        if (CollectionUtils.isNotEmpty(keys)) {
            List<DeviceNotification> notifications = new ArrayList<>();
            for (final String key : keys) {
                notifications.add(getByKey(key));
            }
            return notifications;
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getAllKeysByGuids(Collection<String> guids) {
        if (CollectionUtils.isNotEmpty(guids)) {
            List<String> keys = new ArrayList<>();
            for (String guid : guids) {
                keys.addAll(redis.getAllKeys(String.format(KEY_FORMAT, guid, "*")));
            }
            return keys;
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getAllKeysByIds(Collection<String> ids) {
        if (CollectionUtils.isNotEmpty(ids)) {
            List<String> keys = new ArrayList<>();
            for (String id : ids) {
                keys.addAll(redis.getAllKeys(String.format(KEY_FORMAT, "*", id)));
            }
            return keys;
        }
        return Collections.emptyList();
    }

    @Override
    public List<DeviceNotification> getAll() {
        Set<String> keys = redis.getAllKeys(String.format(KEY_FORMAT, "*", "*"));
        List<DeviceNotification> notifications = new ArrayList<>();
        for (final String key : keys) {
            notifications.add(getByKey(key));
        }
        return notifications;
    }
}

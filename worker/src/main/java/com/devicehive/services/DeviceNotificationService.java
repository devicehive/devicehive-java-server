package com.devicehive.services;

import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.devicehive.domain.DeviceNotification;
import com.devicehive.domain.wrappers.DeviceNotificationWrapper;
import com.devicehive.repository.DeviceNotificationRepository;
import com.devicehive.utils.MessageUtils;
import com.devicehive.utils.mapper.NotificationRowMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

/**
 * Created by tatyana on 2/11/15.
 */
@Service
public class DeviceNotificationService {

    @Autowired
    private CqlOperations cqlTemplate;
    @Autowired
    private DeviceNotificationRepository notificationRepository;
    @Autowired
    private MessageUtils messageUtils;

    public List<DeviceNotification> get(int count, final String commandId, final String deviceGuids, final String notificationNames, final Timestamp timestamp) {
        Select.Where select = QueryBuilder.select().from("device_notification").where();
        if (StringUtils.isNotBlank(deviceGuids)) {
            select.and(QueryBuilder.in("device_guid", messageUtils.getDeviceGuids(deviceGuids)));
        }
        if (StringUtils.isNotBlank(commandId)) {
            select.and(QueryBuilder.in("id", commandId));
        }
        List<DeviceNotification> notifications = cqlTemplate.query(select.limit(count).allowFiltering(), new NotificationRowMapper());
        if (timestamp != null) {
            CollectionUtils.filter(notifications, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    return timestamp.before(((DeviceNotificationWrapper) o).getTimestamp());
                }
            });
        }
        if (StringUtils.isNotBlank(notificationNames)) {
            CollectionUtils.filter(notifications, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    return notificationNames.contains(((DeviceNotificationWrapper) o).getNotification());
                }
            });
        }
        return notifications;
    }

    public Long getNotificationsCount() {
        return notificationRepository.count();
    }

    @Async
    public void delete(String deviceGuids) {
        Delete.Where delete = QueryBuilder.delete().from("device_command").where();
        if (StringUtils.isNotEmpty(deviceGuids)) {
            delete.and(QueryBuilder.in("device_guid", messageUtils.getDeviceGuids(deviceGuids)));
        }
        cqlTemplate.execute(delete);
    }
}

package com.devicehive;

import com.datastax.driver.core.utils.UUIDs;
import com.devicehive.domain.DeviceNotification;
import com.devicehive.repository.DeviceNotificationRepository;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraOperations;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * Created by tmatvienko on 2/2/15.
 */
public class NotificationRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DeviceNotificationRepository notificationRepository;
    @Autowired
    private CassandraOperations cassandraTemplate;

    @Test
    public void repositoryStoresAndRetrievesEvents() {
        final String id = String.valueOf(UUIDs.timeBased().timestamp());
        final DeviceNotification notif1 = new DeviceNotification(id, deviceGuid, date, "notification1", null);
        final DeviceNotification notif2 = new DeviceNotification(String.valueOf(UUIDs.timeBased().timestamp()), deviceGuid2, date, "notification2", null);
        cassandraTemplate.insertAsynchronously(notif1);
        cassandraTemplate.insertAsynchronously(notif2);

        Iterable<DeviceNotification> notifications = notificationRepository.findByDeviceGuid(deviceGuid);

        assertThat(notifications, hasItem(notif1));
        assertThat(notifications, not(hasItem(notif2)));

        notifications = notificationRepository.findByDeviceGuid(deviceGuid2);

        assertThat(notifications, hasItem(notif2));
        assertThat(notifications, not(hasItem(notif1)));

    }

    @Test
    public void repositoryDeletesStoredEvents() {
        final DeviceNotification notif1 = new DeviceNotification(String.valueOf(UUIDs.timeBased()), deviceGuid, date, "notification1", null);
        final DeviceNotification notif2 = new DeviceNotification(String.valueOf(UUIDs.timeBased()), deviceGuid, date, "notification1", null);
        notificationRepository.save(ImmutableSet.of(notif1, notif2));

        notificationRepository.delete(notif1);
        notificationRepository.delete(notif2);

        Iterable<DeviceNotification> notifications = notificationRepository.findByDeviceGuid(deviceGuid);

        assertThat(notifications, not(hasItem(notif1)));
        assertThat(notifications, not(hasItem(notif2)));
    }
}

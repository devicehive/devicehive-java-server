package com.devicehive.repository;

import com.devicehive.domain.DeviceNotification;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

/**
 * Created by tmatvienko on 2/5/15.
 */
public interface DeviceNotificationRepository extends CassandraRepository<DeviceNotification> {

    @Query("select * from device_notification where device_guid in (?0) LIMIT 1000")
    Iterable<DeviceNotification> findByDeviceGuid(String deviceGuids);
}

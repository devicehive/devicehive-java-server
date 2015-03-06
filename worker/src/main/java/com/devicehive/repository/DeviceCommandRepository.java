package com.devicehive.repository;

import com.devicehive.domain.DeviceCommand;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

/**
 * Created by tmatvienko on 2/13/15.
 */
public interface DeviceCommandRepository extends CassandraRepository<DeviceCommand> {

    @Query("select * from device_command where device_guid in (?0) LIMIT 1000")
    Iterable<DeviceCommand> findByDeviceGuids(String deviceGuids);
}

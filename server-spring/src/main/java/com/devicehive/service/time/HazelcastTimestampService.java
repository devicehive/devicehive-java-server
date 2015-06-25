package com.devicehive.service.time;

import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
public class HazelcastTimestampService implements TimestampService {
    @Autowired
    private HazelcastInstance instance;

    @Override
    public java.sql.Timestamp getTimestamp() {
        return new Timestamp(instance.getCluster().getClusterTime());
    }
}

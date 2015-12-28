package com.devicehive.service.time;

import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class HazelcastTimestampService implements TimestampService {
    @Autowired
    private HazelcastInstance instance;

    @Override
    public Date getTimestamp() {
        return new Date(getTime());
    }

    @Override
    public long getTime() {
        return instance.getCluster().getClusterTime();
    }
}

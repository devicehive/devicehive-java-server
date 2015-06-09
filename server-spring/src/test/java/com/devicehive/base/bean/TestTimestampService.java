package com.devicehive.base.bean;

import com.devicehive.service.time.TimestampService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Profile("test")
@Component
public class TestTimestampService implements TimestampService {

    @Override
    public Timestamp getTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

}

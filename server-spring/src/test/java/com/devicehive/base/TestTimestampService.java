package com.devicehive.base;

import com.devicehive.service.TimestampService;
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

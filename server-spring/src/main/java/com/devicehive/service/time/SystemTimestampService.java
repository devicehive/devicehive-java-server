package com.devicehive.service.time;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Profile("test")
@Component
public class SystemTimestampService implements TimestampService {

    @Override
    public Timestamp getTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

}

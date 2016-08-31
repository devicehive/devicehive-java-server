package com.devicehive.service.time;

import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class LocalTimestampService implements TimestampService {
    @Override
    public Date getTimestamp() {
        return new Date();
    }
}

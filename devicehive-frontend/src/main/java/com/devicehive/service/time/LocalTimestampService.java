package com.devicehive.service.time;

import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

@Component
public class LocalTimestampService implements TimestampService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    @Override
    public Date getDate() {
        return new Date();
    }

    @Override
    public String getDateAsString() {
        DATE_FORMAT.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
        return DATE_FORMAT.format(getDate());
    }

    @Override
    public long getTimestamp() {
        return getDate().getTime();
    }

}

package com.devicehive.service.time;


import java.text.SimpleDateFormat;
import java.util.Date;

public interface TimestampService {

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    Date getTimestamp();
}

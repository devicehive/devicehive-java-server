package com.devicehive.service.time;

import java.util.Date;

public interface TimestampService {

    Date getDate();

    String getDateAsString();

    long getTimestamp();
}

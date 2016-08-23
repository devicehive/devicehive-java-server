package com.devicehive.model;

import java.util.Date;

public interface HazelcastEntity {

    String getHazelcastKey();

    Date getTimestamp();
}

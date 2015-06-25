package com.devicehive.model;

import java.sql.Timestamp;

public interface HazelcastEntity {
    String getHazelcastKey();
    Timestamp getTimestamp();
}

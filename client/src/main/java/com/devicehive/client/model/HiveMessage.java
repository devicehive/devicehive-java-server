package com.devicehive.client.model;

import java.sql.Timestamp;

/**
 * Created by stas on 05.07.14.
 */
public interface HiveMessage extends HiveEntity {

    public Timestamp getTimestamp();
}

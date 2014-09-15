package com.devicehive.client.model;

import java.io.Serializable;

/**
 * Interface that marks Device Hive entity to be available for serialization
 */
public interface HiveEntity extends Serializable {

    public final static long INITIAL_ENTITY_VERSION = 0L;

}

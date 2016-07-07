package com.devicehive.dao;

import com.devicehive.model.DeviceClass;

/**
 * Created by Gleb on 07.07.2016.
 */
public interface DeviceClassDao {
    DeviceClass findByNameAndVersion(String name, String version);
}

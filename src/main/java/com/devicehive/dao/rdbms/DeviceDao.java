package com.devicehive.dao.rdbms;

import com.devicehive.model.Device;

/**
 * Created by Gleb on 07.07.2016.
 */
public interface DeviceDao {
    Device findByUUID(String uuid);
}

package com.devicehive.dao;

import com.devicehive.model.DeviceClass;

import java.util.List;

public interface DeviceClassDao {

    void remove(long id);

    DeviceClass find(long id);

    void persist(DeviceClass deviceClass);

    DeviceClass merge(DeviceClass deviceClass);

    List<DeviceClass> getDeviceClassList(String name, String namePattern, String sortField,
                                                Boolean sortOrderAsc, Integer take, Integer skip);
    DeviceClass findByName(String name);
}

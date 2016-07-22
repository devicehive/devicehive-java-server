package com.devicehive.dao;

import com.devicehive.model.DeviceClass;

import javax.persistence.LockModeType;
import java.util.List;

public interface DeviceClassDao {

    DeviceClass getReference(Long id);

    void remove(DeviceClass reference);

    DeviceClass find(Long id);

    void persist(DeviceClass deviceClass);

    DeviceClass merge(DeviceClass deviceClass);

    List<DeviceClass> getDeviceClassList(String name, String namePattern, String sortField,
                                                Boolean sortOrderAsc, Integer take, Integer skip);
    DeviceClass findByName(String name);
}

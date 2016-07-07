package com.devicehive.dao;

import com.devicehive.model.DeviceClass;

import javax.persistence.LockModeType;
import java.util.List;

/**
 * Created by Gleb on 07.07.2016.
 */
public interface DeviceClassDao {

    DeviceClass findByNameAndVersion(String name, String version);

    boolean isExist(long id);

    DeviceClass getReference(long id);

    void remove(DeviceClass reference);

    DeviceClass find(long id);

    void refresh(DeviceClass stored, LockModeType lockModeType);

    void persist(DeviceClass deviceClass);

    DeviceClass merge(DeviceClass deviceClass);

    public List<DeviceClass> getDeviceClassList(String name, String namePattern, String version, String sortField,
                                                Boolean sortOrderAsc, Integer take, Integer skip);
}

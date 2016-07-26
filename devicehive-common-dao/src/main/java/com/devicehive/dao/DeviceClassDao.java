package com.devicehive.dao;

import com.devicehive.model.DeviceClass;
import com.devicehive.vo.DeviceClassReferenceVO;

import java.util.List;

public interface DeviceClassDao {

    DeviceClassReferenceVO getReference(Long id);

    void remove(DeviceClassReferenceVO reference);

    DeviceClass find(Long id);

    void persist(DeviceClass deviceClass);

    DeviceClass merge(DeviceClass deviceClass);

    List<DeviceClass> getDeviceClassList(String name, String namePattern, String sortField,
                                                Boolean sortOrderAsc, Integer take, Integer skip);
    DeviceClass findByName(String name);
}

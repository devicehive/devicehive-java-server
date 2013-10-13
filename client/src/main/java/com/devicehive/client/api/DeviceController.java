package com.devicehive.client.api;


import com.devicehive.client.model.Device;
import com.devicehive.client.model.DeviceClass;
import com.devicehive.client.model.DeviceEquipment;

import java.util.List;

public interface DeviceController {

    //device block
    List<Device> listDevices(String name, String namePattern, String status, Integer networkId, String networkName,
                             Integer deviceClassId, String deviceClassName, String deviceClassVersion,
                             String sortField, String sortOrder, Integer take, Integer skip);

    Device getDevice(String guid);

    void registerDevice(String guid, Device device);

    void deleteDevice(String guid);

    DeviceEquipment getDeviceEquipment(String guid);

    //device class block
    List<DeviceClass> listDeviceClass(String name, String namePattern, String version, String sortField,
                                      String sortOrder, Integer take, Integer skip);

    DeviceClass getDeviceClass(long classId);

    long insertDeviceClass(DeviceClass deviceClass);

    void updateDeviceClass(long classId, DeviceClass deviceClass);

    void deleteDeviceClass(long classId);
}

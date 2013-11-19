package com.devicehive.client.api.client;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.DeviceClass;
import com.devicehive.client.model.DeviceEquipment;
import com.google.common.reflect.TypeToken;

import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceControllerImpl implements DeviceController {

    private final HiveContext hiveContext;

    public DeviceControllerImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    //for devices
    @Override
    public List<Device> listDevices(String name, String namePattern, String status, Integer networkId,
                                    String networkName, Integer deviceClassId, String deviceClassName,
                                    String deviceClassVersion, String sortField, String sortOrder, Integer take,
                                    Integer skip) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("name", name);
        queryParams.put("namePattern", namePattern);
        queryParams.put("status", status);
        queryParams.put("networkId", networkId);
        queryParams.put("networkName", networkName);
        queryParams.put("deviceClassId", deviceClassId);
        queryParams.put("deviceClassName", deviceClassName);
        queryParams.put("deviceClassVersion", deviceClassVersion);
        queryParams.put("sortField", sortField);
        queryParams.put("sortOrder", sortOrder);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        String path = "/device";
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, queryParams,
                new TypeToken<List<Device>>() {
                }.getType(), DEVICE_PUBLISHED);
    }

    @Override
    public Device getDevice(String deviceId) {
        String path = "/device/" + deviceId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, Device.class, DEVICE_PUBLISHED);
    }

    @Override
    public void registerDevice(String deviceId, Device device) {
        String path = "/device/" + deviceId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, device, DEVICE_PUBLISHED);
    }

    @Override
    public void deleteDevice(String deviceId) {
        String path = "/device/" + deviceId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.DELETE);
    }

    @Override
    public List<DeviceEquipment> getDeviceEquipment(String deviceId) {
        String path = "/device/" + deviceId + "/equipment";
        return hiveContext.getHiveRestClient()
                .execute(path, HttpMethod.GET, null, null, null, new TypeToken<List<DeviceEquipment>>() {
                }.getType(), null, DEVICE_EQUIPMENT_SUBMITTED);
    }

    //for device classes
    @Override
    public List<DeviceClass> listDeviceClass(String name, String namePattern, String version, String sortField,
                                             String sortOrder, Integer take, Integer skip) {
        String path = "/device/class";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("name", name);
        queryParams.put("namePattern", namePattern);
        queryParams.put("version", version);
        queryParams.put("sortField", sortField);
        queryParams.put("sortOrder", sortOrder);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, queryParams,
                new TypeToken<List<DeviceClass>>() {
                }.getType(), DEVICECLASS_LISTED);
    }

    @Override
    public DeviceClass getDeviceClass(long classId) {
        String path = "/device/class/" + classId;
        return hiveContext.getHiveRestClient()
                .execute(path, HttpMethod.GET, null, DeviceClass.class, DEVICECLASS_PUBLISHED);
    }

    @Override
    public long insertDeviceClass(DeviceClass deviceClass) {
        String path = "/device/class";
        DeviceClass inserted = hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, deviceClass,
                DeviceClass.class, DEVICECLASS_PUBLISHED, DEVICECLASS_SUBMITTED);
        return inserted.getId();
    }

    @Override
    public void updateDeviceClass(long classId, DeviceClass deviceClass) {
        String path = "/device/class/" + classId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, deviceClass, DEVICECLASS_PUBLISHED);
    }

    @Override
    public void deleteDeviceClass(long classId) {
        String path = "/device/class" + classId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.DELETE);
    }
}

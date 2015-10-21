package com.devicehive.base.fixture;

import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;

import java.util.Optional;
import java.util.UUID;

public class DeviceFixture {

    public static Equipment createEquipment() {
        UUID uuid = UUID.randomUUID();
        Equipment equipment = new Equipment();
        equipment.setName("equipment-" + uuid);
        equipment.setCode("equipment_code-" + uuid);
        equipment.setType("equipment_type-" + uuid);
        equipment.setData(new JsonStringWrapper(String.format("{\"data\": \"equipment_data-%s\"}", uuid)));
        return equipment;
    }

    public static DeviceClassUpdate createDeviceClass() {
        UUID uuid = UUID.randomUUID();
        DeviceClassUpdate deviceClass = new DeviceClassUpdate();
        deviceClass.setName(Optional.ofNullable("device_class-" + uuid));
        deviceClass.setVersion(Optional.ofNullable("1"));
        deviceClass.setPermanent(Optional.ofNullable(false));
        deviceClass.setOfflineTimeout(Optional.ofNullable(120));
        deviceClass.setData(Optional.ofNullable(new JsonStringWrapper(String.format("{\"data\": \"device_class_data-%s\"}", uuid))));
        return deviceClass;
    }
    public static DeviceClassUpdate createDeviceClassUpdate(DeviceClass dc) {
        UUID uuid = UUID.randomUUID();
        DeviceClassUpdate deviceClass = new DeviceClassUpdate();
        deviceClass.setId(dc.getId());
        deviceClass.setName(Optional.ofNullable(dc.getName()));
        deviceClass.setVersion(Optional.ofNullable(dc.getVersion()));
        deviceClass.setPermanent(Optional.ofNullable(false));
        deviceClass.setOfflineTimeout(Optional.ofNullable(dc.getOfflineTimeout()));
        deviceClass.setData(Optional.ofNullable(new JsonStringWrapper(String.format("{\"data\": \"device_class_data-%s\"}", uuid))));
        return deviceClass;
    }

    public static DeviceClass createDC() {
        UUID uuid = UUID.randomUUID();
        DeviceClass deviceClass = new DeviceClass();
        deviceClass.setName("device_class-" + uuid);
        deviceClass.setVersion("1");
        deviceClass.setPermanent(false);
        deviceClass.setOfflineTimeout(120);
        deviceClass.setData(new JsonStringWrapper(String.format("{\"data\": \"device_class_data-%s\"}", uuid)));

        return deviceClass;
    }

    public static Network createNetwork() {
        UUID uuid = UUID.randomUUID();
        Network network = new Network();
        network.setName("network-" + uuid);
        network.setDescription("network_description-" + uuid);
        return network;
    }

    public static DeviceUpdate createDevice(String guid) {
        DeviceUpdate device = new DeviceUpdate();
        device.setGuid(new NullableWrapper<>(guid));
        device.setName(new NullableWrapper<>("device-" + guid));
        device.setStatus(new NullableWrapper<>("Online"));
        device.setData(new NullableWrapper<>(new JsonStringWrapper(String.format("{\"data\": \"device_data-%s\"}", guid))));
        return device;
    }

    public static DeviceUpdate createDevice(String guid, DeviceClassUpdate dc) {
        final DeviceUpdate deviceUpdate = createDevice(guid);
        deviceUpdate.setDeviceClass(new NullableWrapper<>(dc));
        return deviceUpdate;
    }

    public static DeviceUpdate createDevice(Device device, DeviceClassUpdate dc) {
        final DeviceUpdate deviceUpdate = new DeviceUpdate();
        deviceUpdate.setGuid(new NullableWrapper<>(device.getGuid()));
        deviceUpdate.setName(new NullableWrapper<>(device.getName()));
        deviceUpdate.setStatus(new NullableWrapper<>(device.getStatus()));
        deviceUpdate.setDeviceClass(new NullableWrapper<>(dc));
        return deviceUpdate;
    }

    public static Device createDevice() {
        final UUID uuid = UUID.randomUUID();
        final Device device = new Device();
        device.setGuid(uuid.toString());
        device.setName("name-"+uuid.toString());
        return device;
    }

}

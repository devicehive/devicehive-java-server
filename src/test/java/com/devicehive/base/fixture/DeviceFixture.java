package com.devicehive.base.fixture;

import com.devicehive.model.Equipment;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.Network;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;

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
        deviceClass.setName(new NullableWrapper<>("device_class-" + uuid));
        deviceClass.setVersion(new NullableWrapper<>("1"));
        deviceClass.setPermanent(new NullableWrapper<>(false));
        deviceClass.setOfflineTimeout(new NullableWrapper<>(120));
        deviceClass.setData(new NullableWrapper<>(new JsonStringWrapper(String.format("{\"data\": \"device_class_data-%s\"}", uuid))));
        return deviceClass;
    }

    public static Network createNetwork() {
        UUID uuid = UUID.randomUUID();
        Network network = new Network();
        network.setName("network-" + uuid);
        network.setDescription("network_description-" + uuid);
        return network;
    }

    public static DeviceUpdate createDevice(String deviceKey) {
        DeviceUpdate device = new DeviceUpdate();
        device.setKey(new NullableWrapper<>(deviceKey));
        device.setName(new NullableWrapper<>("device-" + deviceKey));
        device.setStatus(new NullableWrapper<>("Online"));
        device.setData(new NullableWrapper<>(new JsonStringWrapper(String.format("{\"data\": \"device_data-%s\"}", deviceKey))));
        return device;
    }

}

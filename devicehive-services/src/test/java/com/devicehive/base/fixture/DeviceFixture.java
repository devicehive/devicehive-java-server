package com.devicehive.base.fixture;

import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.Network;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;
import com.devicehive.vo.NetworkVO;

import java.util.Optional;
import java.util.UUID;

public class DeviceFixture {

    public static DeviceClassEquipmentVO createEquipmentVO() {
        UUID uuid = UUID.randomUUID();
        DeviceClassEquipmentVO equipment = new DeviceClassEquipmentVO();
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
        deviceClass.setPermanent(Optional.ofNullable(false));
        deviceClass.setOfflineTimeout(Optional.ofNullable(120));
        deviceClass.setData(Optional.ofNullable(new JsonStringWrapper(String.format("{\"data\": \"device_class_data-%s\"}", uuid))));
        return deviceClass;
    }
    public static DeviceClassUpdate createDeviceClassUpdate(DeviceClassWithEquipmentVO dc) {
        UUID uuid = UUID.randomUUID();
        DeviceClassUpdate deviceClass = new DeviceClassUpdate();
        deviceClass.setId(dc.getId());
        deviceClass.setName(Optional.ofNullable(dc.getName()));
        deviceClass.setPermanent(Optional.ofNullable(false));
        deviceClass.setOfflineTimeout(Optional.ofNullable(dc.getOfflineTimeout()));
        deviceClass.setData(Optional.ofNullable(new JsonStringWrapper(String.format("{\"data\": \"device_class_data-%s\"}", uuid))));
        return deviceClass;
    }

    public static DeviceClassWithEquipmentVO createDCVO() {
        UUID uuid = UUID.randomUUID();
        DeviceClassWithEquipmentVO deviceClass = new DeviceClassWithEquipmentVO();
        deviceClass.setName("device_class-" + uuid);
        deviceClass.setIsPermanent(false);
        deviceClass.setOfflineTimeout(120);
        deviceClass.setData(new JsonStringWrapper(String.format("{\"data\": \"device_class_data-%s\"}", uuid)));

        return deviceClass;
    }

    public static NetworkVO createNetwork() {
        UUID uuid = UUID.randomUUID();
        NetworkVO network = new NetworkVO();
        network.setName("network-" + uuid);
        network.setDescription("network_description-" + uuid);
        return network;
    }

    public static DeviceUpdate createDevice(String guid) {
        DeviceUpdate device = new DeviceUpdate();
        device.setGuid(Optional.ofNullable(guid));
        device.setName(Optional.ofNullable("device-" + guid));
        device.setStatus(Optional.ofNullable("Online"));
        device.setData(Optional.ofNullable(new JsonStringWrapper(String.format("{\"data\": \"device_data-%s\"}", guid))));
        return device;
    }

    public static DeviceUpdate createDevice(String deviceKey, DeviceClassUpdate dc) {
        final DeviceUpdate deviceUpdate = createDevice(deviceKey);
        deviceUpdate.setDeviceClass(Optional.ofNullable(dc));
        return deviceUpdate;
    }

    public static DeviceUpdate createDevice(Device device, DeviceClassUpdate dc) {
        final DeviceUpdate deviceUpdate = new DeviceUpdate();
        deviceUpdate.setGuid(Optional.ofNullable(device.getGuid()));
        deviceUpdate.setName(Optional.ofNullable(device.getName()));
        deviceUpdate.setStatus(Optional.ofNullable(device.getStatus()));
        deviceUpdate.setDeviceClass(Optional.ofNullable(dc));
        return deviceUpdate;
    }

    public static Device createDevice() {
        final UUID uuid = UUID.randomUUID();
        final Device device = new Device();
        device.setGuid(uuid.toString());
        device.setName("name-" + uuid.toString());
        return device;
    }

    public static DeviceCommand createDeviceCommand() {
        DeviceCommand command = new DeviceCommand();
        command.setCommand("test-command");
        command.setParameters(new JsonStringWrapper("{'param':'testParam'}"));
        command.setLifetime(0);
        command.setStatus("test-status");
        command.setResult(new JsonStringWrapper("{'jsonString': 'string'}"));
        return command;
    }

}
